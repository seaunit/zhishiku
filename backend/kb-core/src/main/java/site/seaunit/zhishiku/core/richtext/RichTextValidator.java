package site.seaunit.zhishiku.core.richtext;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class RichTextValidator {
  private static final Set<String> BLOCK_TYPES =
      Set.of(
          "paragraph",
          "heading",
          "bulletList",
          "orderedList",
          "blockquote",
          "codeBlock",
          "horizontalRule",
          "image",
          "table");
  private static final Set<String> INLINE_TYPES = Set.of("text", "hardBreak");
  private static final Set<String> MARK_TYPES =
      Set.of("bold", "italic", "underline", "strike", "code", "link");
  private static final Set<String> IMAGE_ATTRS =
      Set.of(
          "assetId",
          "uploadId",
          "nodeId",
          "alt",
          "caption",
          "displayWidthPx",
          "aspectRatio",
          "alignment",
          "decorative");
  private static final Set<String> ALIGNMENTS = Set.of("left", "center", "right");
  private static final Pattern CODE_LANGUAGE = Pattern.compile("^[A-Za-z0-9_+-]{0,40}$");

  private final RichTextLimits limits;

  public RichTextValidator(RichTextLimits limits) {
    this.limits = limits;
  }

  public static RichTextValidator defaults() {
    return new RichTextValidator(RichTextLimits.defaults());
  }

  public RichTextValidation validate(JsonNode document) {
    if (document == null || !document.isObject()) {
      throw invalid("DOCUMENT_REQUIRED", "Rich text document must be an object.");
    }
    requireOnlyFields(document, Set.of("type", "content"), "UNKNOWN_DOCUMENT_FIELD");
    if (!"doc".equals(document.path("type").asText())) {
      throw invalid("DOCUMENT_TYPE", "Root node type must be doc.");
    }
    JsonNode content = document.get("content");
    if (content == null || !content.isArray()) {
      throw invalid("DOCUMENT_CONTENT", "Document content must be an array.");
    }

    Counter counter = new Counter();
    validateChildren(content, 2, counter);
    if (counter.characters > limits.maxCharacters()) {
      throw invalid("CONTENT_TOO_LARGE", "Visible text exceeds the supported limit.");
    }
    if (counter.images > limits.maxImages()) {
      throw invalid("TOO_MANY_IMAGES", "A knowledge item may contain at most 20 images.");
    }
    return new RichTextValidation(
        RichTextSchema.CURRENT_VERSION,
        counter.imageAssets.size(),
        counter.imageAssets,
        List.copyOf(counter.uniqueAssetIds));
  }

  private void validateNode(JsonNode node, int depth, Counter counter, String parentType) {
    if (depth > limits.maxDepth()) {
      throw invalid("DEPTH_EXCEEDED", "Rich text nesting is too deep.");
    }
    if (!node.isObject()) {
      throw invalid("NODE_OBJECT", "Every rich text node must be an object.");
    }

    String type = requiredText(node, "type");
    if (parentType != null
        && Set.of("bulletList", "orderedList").contains(parentType)
        && !"listItem".equals(type)) {
      throw invalid("LIST_CHILD", "Lists may only contain listItem nodes.");
    }
    if (parentType != null
        && "blockquote".equals(parentType)
        && !Set.of("paragraph", "heading").contains(type)) {
      throw invalid("BLOCKQUOTE_CHILD", "Block quotes may only contain paragraphs or headings.");
    }
    if (parentType != null
        && Set.of("tableHeader", "tableCell").contains(parentType)
        && !"paragraph".equals(type)) {
      throw invalid("TABLE_CELL_CHILD", "Table cells may only contain paragraphs.");
    }
    if (BLOCK_TYPES.contains(type)) {
      counter.blocks++;
      if (counter.blocks > limits.maxBlocks()) {
        throw invalid("TOO_MANY_BLOCKS", "Rich text contains too many blocks.");
      }
      validateBlock(node, type, depth, counter, parentType);
      return;
    }
    if (INLINE_TYPES.contains(type)) {
      validateInline(node, type, counter);
      return;
    }
    if (Set.of("listItem", "tableHeaderRow", "tableBodyRow", "tableHeader", "tableCell")
        .contains(type)) {
      validateContainer(node, type, depth, counter, parentType);
      return;
    }
    throw invalid("UNKNOWN_NODE", "Unsupported rich text node: " + type);
  }

  private void validateBlock(
      JsonNode node, String type, int depth, Counter counter, String parentType) {
    switch (type) {
      case "paragraph" -> {
        requireOnlyFields(node, Set.of("type", "attrs", "content"), "UNKNOWN_NODE_FIELD");
        JsonNode attrs = node.get("attrs");
        if (attrs != null) {
          if (!attrs.isObject()) {
            throw invalid("PARAGRAPH_ATTRS", "Paragraph attributes must be an object.");
          }
          requireOnlyFields(attrs, Set.of("textAlign"), "UNKNOWN_ATTRIBUTE");
          validateAlignment(attrs);
        }
        validateInlineChildren(node, depth, counter);
      }
      case "heading" -> {
        requireOnlyFields(node, Set.of("type", "attrs", "content"), "UNKNOWN_NODE_FIELD");
        JsonNode attrs = requiredObject(node, "attrs");
        requireOnlyFields(attrs, Set.of("level", "textAlign"), "UNKNOWN_ATTRIBUTE");
        int level = node.path("attrs").path("level").asInt();
        if (level < 2 || level > 4) {
          throw invalid("HEADING_LEVEL", "Heading level must be between 2 and 4.");
        }
        validateAlignment(attrs);
        validateInlineChildren(node, depth, counter);
      }
      case "bulletList", "orderedList" -> {
        requireOnlyFields(node, Set.of("type", "attrs", "content"), "UNKNOWN_NODE_FIELD");
        if ("orderedList".equals(type)) {
          JsonNode attrs = node.get("attrs");
          if (attrs != null) {
            if (!attrs.isObject()) {
              throw invalid("ORDERED_LIST_ATTRS", "Ordered list attributes must be an object.");
            }
            requireOnlyFields(attrs, Set.of("start"), "UNKNOWN_ATTRIBUTE");
            if (attrs.has("start")
                && (!attrs.get("start").canConvertToInt()
                    || attrs.get("start").asInt() < 1
                    || attrs.get("start").asInt() > 9999)) {
              throw invalid("ORDERED_LIST_START", "Ordered list start must be between 1 and 9999.");
            }
          }
        }
        JsonNode children = requiredArray(node, "content");
        for (JsonNode child : children) {
          validateNode(child, depth + 1, counter, type);
        }
      }
      case "blockquote" -> {
        for (JsonNode child : requiredArray(node, "content")) {
          validateNode(child, depth + 1, counter, "blockquote");
        }
      }
      case "codeBlock" -> {
        requireOnlyFields(node, Set.of("type", "attrs", "content"), "UNKNOWN_NODE_FIELD");
        JsonNode attrs = node.get("attrs");
        if (attrs != null) {
          if (!attrs.isObject()) {
            throw invalid("CODE_BLOCK_ATTRS", "Code block attributes must be an object.");
          }
          requireOnlyFields(attrs, Set.of("language"), "UNKNOWN_ATTRIBUTE");
          if (attrs.has("language")
              && (!attrs.get("language").isTextual()
                  || !CODE_LANGUAGE.matcher(attrs.get("language").asText()).matches())) {
            throw invalid("CODE_LANGUAGE", "Code block language is invalid.");
          }
        }
        JsonNode children = node.get("content");
        if (children != null) {
          if (!children.isArray()) {
            throw invalid("CODE_BLOCK_CONTENT", "Code block content must be an array.");
          }
          for (JsonNode child : children) {
            String childType = requiredText(child, "type");
            if (!"text".equals(childType)) {
              throw invalid("CODE_BLOCK_CHILD", "Code blocks may only contain text.");
            }
            requireOnlyFields(child, Set.of("type", "text"), "UNKNOWN_NODE_FIELD");
            addText(counter, child.path("text").asText(""));
          }
        }
      }
      case "horizontalRule" -> {
        requireOnlyFields(node, Set.of("type"), "UNKNOWN_NODE_FIELD");
      }
      case "image" -> validateImage(node, counter);
      case "table" -> validateTable(node, depth, counter);
      default -> throw invalid("UNKNOWN_NODE", "Unsupported block node: " + type);
    }
    if ("listItem".equals(parentType) && !isAllowedListItemBlock(type)) {
      throw invalid("LIST_ITEM_CHILD", "List items contain only paragraphs or nested lists.");
    }
  }

  private void validateContainer(
      JsonNode node, String type, int depth, Counter counter, String parentType) {
    if ("listItem".equals(type)) {
      requireOnlyFields(node, Set.of("type", "content"), "UNKNOWN_NODE_FIELD");
    } else if ("tableHeaderRow".equals(type) || "tableBodyRow".equals(type)) {
      requireOnlyFields(node, Set.of("type", "content"), "UNKNOWN_NODE_FIELD");
    } else {
      requireOnlyFields(node, Set.of("type", "attrs", "content"), "UNKNOWN_NODE_FIELD");
      JsonNode attrs = node.get("attrs");
      if (attrs != null) {
        if (!attrs.isObject()) {
          throw invalid("TABLE_CELL_ATTRS", "Table cell attributes must be an object.");
        }
        requireOnlyFields(attrs, Set.of("colspan", "rowspan"), "UNKNOWN_ATTRIBUTE");
        if (attrs.has("colspan")
            && (!attrs.get("colspan").canConvertToInt()
                || attrs.get("colspan").asInt() < 1
                || attrs.get("colspan").asInt() > 20)) {
          throw invalid("TABLE_COLSPAN", "Table cell colspan is invalid.");
        }
        if (attrs.has("rowspan")
            && (!attrs.get("rowspan").canConvertToInt()
                || attrs.get("rowspan").asInt() < 1
                || attrs.get("rowspan").asInt() > 50)) {
          throw invalid("TABLE_ROWSPAN", "Table cell rowspan is invalid.");
        }
      }
    }
    if ("listItem".equals(type) && !Set.of("bulletList", "orderedList").contains(parentType)) {
      throw invalid("LIST_ITEM_PARENT", "List item must be inside a list.");
    }
    if (Set.of("tableHeaderRow", "tableBodyRow").contains(type) && !"table".equals(parentType)) {
      throw invalid("TABLE_ROW_PARENT", "Table row must be inside a table.");
    }
    if (Set.of("tableHeader", "tableCell").contains(type)
        && !Set.of("tableHeaderRow", "tableBodyRow").contains(parentType)) {
      throw invalid("TABLE_CELL_PARENT", "Table cell must be inside a row.");
    }
    JsonNode children = requiredArray(node, "content");
    for (JsonNode child : children) {
      validateNode(child, depth + 1, counter, type);
    }
  }

  private void validateInlineChildren(JsonNode node, int depth, Counter counter) {
    JsonNode children = node.get("content");
    if (children == null) {
      return;
    }
    if (!children.isArray()) {
      throw invalid("INLINE_CONTENT", "Inline content must be an array.");
    }
    for (JsonNode child : children) {
      String childType = requiredText(child, "type");
      if (!INLINE_TYPES.contains(childType)) {
        throw invalid("INLINE_CHILD", "Paragraph and heading content must be inline.");
      }
      validateNode(child, depth + 1, counter, node.path("type").asText());
    }
  }

  private void validateChildren(JsonNode children, int depth, Counter counter) {
    if (!children.isArray()) {
      throw invalid("CHILD_ARRAY", "Node content must be an array.");
    }
    for (JsonNode child : children) {
      validateNode(child, depth, counter, null);
    }
  }

  private void validateInline(JsonNode node, String type, Counter counter) {
    if ("text".equals(type)) {
      requireOnlyFields(node, Set.of("type", "text", "marks"), "UNKNOWN_NODE_FIELD");
      String text = node.path("text").asText("");
      addText(counter, text);
      JsonNode marks = node.get("marks");
      if (marks != null) {
        if (!marks.isArray() || marks.size() > 6) {
          throw invalid("MARKS", "Text marks must be a bounded array.");
        }
        Set<String> seen = new java.util.HashSet<>();
        for (JsonNode mark : marks) {
          String markType = requiredText(mark, "type");
          if (!MARK_TYPES.contains(markType) || !seen.add(markType)) {
            throw invalid("UNKNOWN_MARK", "Unsupported or duplicate text mark: " + markType);
          }
          if ("link".equals(markType)) {
            requireOnlyFields(mark, Set.of("type", "attrs"), "UNKNOWN_NODE_FIELD");
            JsonNode attrs = requiredObject(mark, "attrs");
            requireOnlyFields(attrs, Set.of("href"), "UNKNOWN_ATTRIBUTE");
            String href = requiredText(mark.path("attrs"), "href");
            validateHttpUrl(href);
          } else {
            requireOnlyFields(mark, Set.of("type"), "UNKNOWN_NODE_FIELD");
          }
        }
      }
    } else if ("hardBreak".equals(type)) {
      requireOnlyFields(node, Set.of("type"), "UNKNOWN_NODE_FIELD");
    }
  }

  private void validateImage(JsonNode node, Counter counter) {
    requireOnlyFields(node, Set.of("type", "attrs"), "UNKNOWN_NODE_FIELD");
    JsonNode attrs = requiredObject(node, "attrs");
    requireOnlyAttributes(attrs, IMAGE_ATTRS);
    UUID assetId = parseUuid(attrs, "assetId");
    if (attrs.hasNonNull("uploadId")) {
      UUID uploadId = parseUuid(attrs, "uploadId");
      if (!assetId.equals(uploadId)) {
        throw invalid(
            "IMAGE_ASSET_CONFLICT", "assetId and uploadId must identify the same temporary image.");
      }
    }
    String nodeId = requiredText(attrs, "nodeId");
    if (nodeId.isBlank() || nodeId.length() > 80) {
      throw invalid("IMAGE_NODE_ID", "Image nodeId must be between 1 and 80 characters.");
    }
    if (!counter.nodeIds.add(nodeId)) {
      throw invalid("IMAGE_NODE_ID_DUPLICATE", "Image nodeId must be unique in a document.");
    }
    if (!attrs.path("displayWidthPx").isIntegralNumber()) {
      throw invalid("IMAGE_WIDTH", "Image display width must be an integer.");
    }
    int width = attrs.path("displayWidthPx").asInt();
    if (width < 80 || width > limits.maxDisplayWidth()) {
      throw invalid("IMAGE_WIDTH", "Image display width is outside the supported range.");
    }
    if (!attrs.path("aspectRatio").isNumber()) {
      throw invalid("IMAGE_ASPECT", "Image aspect ratio must be numeric.");
    }
    double aspectRatio = attrs.path("aspectRatio").asDouble();
    if (aspectRatio < 0.01 || aspectRatio > 100) {
      throw invalid("IMAGE_ASPECT", "Image aspect ratio is invalid.");
    }
    if (!ALIGNMENTS.contains(attrs.path("alignment").asText())) {
      throw invalid("IMAGE_ALIGN", "Image alignment is invalid.");
    }
    if (!attrs.path("decorative").isBoolean()) {
      throw invalid("IMAGE_DECORATIVE", "Image decorative flag must be boolean.");
    }
    boolean decorative = attrs.path("decorative").asBoolean();
    if (!decorative && attrs.path("alt").asText("").isBlank()) {
      throw invalid("IMAGE_ALT_REQUIRED", "Non-decorative images require alt text.");
    }
    checkMaxLength(attrs.path("alt"), 500, "IMAGE_ALT_LENGTH");
    checkMaxLength(attrs.path("caption"), 500, "IMAGE_CAPTION_LENGTH");
    counter.images++;
    counter.uniqueAssetIds.add(assetId);
    counter.imageAssets.add(
        new RichTextImageAsset(
            assetId,
            nodeId,
            attrs.path("alt").asText(""),
            attrs.path("caption").asText(""),
            width,
            aspectRatio,
            attrs.path("alignment").asText(),
            decorative));
  }

  private void validateTable(JsonNode node, int depth, Counter counter) {
    requireOnlyFields(node, Set.of("type", "attrs", "content"), "UNKNOWN_NODE_FIELD");
    JsonNode attrs = requiredObject(node, "attrs");
    requireOnlyFields(attrs, Set.of("headerRow"), "UNKNOWN_ATTRIBUTE");
    if (!attrs.path("headerRow").isBoolean() || !attrs.path("headerRow").asBoolean()) {
      throw invalid("TABLE_HEADER_REQUIRED", "Table headerRow must be true.");
    }
    JsonNode rows = requiredArray(node, "content");
    if (rows.isEmpty()) {
      throw invalid("TABLE_EMPTY", "Table must contain a header row.");
    }
    for (int index = 0; index < rows.size(); index++) {
      JsonNode row = rows.get(index);
      String expected = index == 0 ? "tableHeaderRow" : "tableBodyRow";
      if (!expected.equals(row.path("type").asText())) {
        throw invalid(
            "TABLE_ROW_ORDER", "First row must be tableHeaderRow and later rows tableBodyRow.");
      }
      JsonNode cells = requiredArray(row, "content");
      String expectedCell = index == 0 ? "tableHeader" : "tableCell";
      for (JsonNode cell : cells) {
        if (!expectedCell.equals(cell.path("type").asText())) {
          throw invalid("TABLE_CELL_TYPE", "Table header/data cell types do not match the row.");
        }
        validateContainer(cell, expectedCell, depth + 2, counter, expected);
      }
    }
  }

  private static boolean isAllowedListItemBlock(String type) {
    return Set.of("paragraph", "bulletList", "orderedList").contains(type);
  }

  private static void requireOnlyAttributes(JsonNode attrs, Set<String> allowed) {
    Iterator<Map.Entry<String, JsonNode>> fields = attrs.fields();
    while (fields.hasNext()) {
      String name = fields.next().getKey();
      if (!allowed.contains(name)) {
        throw invalid("UNKNOWN_ATTRIBUTE", "Unsupported image attribute: " + name);
      }
    }
  }

  private static void validateHttpUrl(String raw) {
    try {
      RichTextUrlPolicy.validateAndNormalize(raw);
    } catch (RichTextException error) {
      throw invalid("UNSAFE_LINK", "Links must use an absolute HTTP or HTTPS URL.");
    }
  }

  private static UUID parseUuid(JsonNode attrs, String field) {
    String raw = requiredText(attrs, field);
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException error) {
      throw invalid(
          "IMAGE_ASSET_ID",
          "Image asset references must be UUIDs; remote URLs and pseudo protocols are not"
              + " allowed.");
    }
  }

  private static void validateAlignment(JsonNode attrs) {
    if (attrs.has("textAlign") && !ALIGNMENTS.contains(attrs.path("textAlign").asText())) {
      throw invalid("TEXT_ALIGNMENT", "Text alignment must be left, center or right.");
    }
  }

  private static void requireOnlyFields(JsonNode node, Set<String> allowed, String code) {
    if (node == null || !node.isObject()) {
      throw invalid("NODE_OBJECT", "Every rich text node must be an object.");
    }
    Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
    while (fields.hasNext()) {
      String name = fields.next().getKey();
      if (!allowed.contains(name)) {
        throw invalid(code, "Unsupported field: " + name);
      }
    }
  }

  private static String requiredText(JsonNode node, String field) {
    if (node == null || !node.hasNonNull(field) || !node.get(field).isTextual()) {
      throw invalid("FIELD_REQUIRED", "Required text field is missing: " + field);
    }
    return node.get(field).asText();
  }

  private static JsonNode requiredArray(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || !value.isArray()) {
      throw invalid("ARRAY_REQUIRED", "Required array field is missing: " + field);
    }
    return value;
  }

  private static JsonNode requiredObject(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || !value.isObject()) {
      throw invalid("OBJECT_REQUIRED", "Required object field is missing: " + field);
    }
    return value;
  }

  private static void addText(Counter counter, String text) {
    if (text == null) {
      return;
    }
    counter.characters += text.codePointCount(0, text.length());
  }

  private static void checkMaxLength(JsonNode node, int max, String code) {
    if (node != null && node.isTextual() && node.asText().length() > max) {
      throw invalid(code, "Text exceeds the supported length.");
    }
  }

  private static RichTextException invalid(String code, String message) {
    return new RichTextException(code, message);
  }

  private static final class Counter {
    private int blocks;
    private int images;
    private int characters;
    private final Set<UUID> uniqueAssetIds = new LinkedHashSet<>();
    private final Set<String> nodeIds = new HashSet<>();
    private final List<RichTextImageAsset> imageAssets = new ArrayList<>();
  }
}

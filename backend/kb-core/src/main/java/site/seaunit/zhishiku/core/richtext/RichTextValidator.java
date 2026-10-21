package site.seaunit.zhishiku.core.richtext;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

  private final RichTextLimits limits;

  public RichTextValidator(RichTextLimits limits) {
    this.limits = limits;
  }

  public static RichTextValidator defaults() {
    return new RichTextValidator(RichTextLimits.defaults());
  }

  public void validate(JsonNode document) {
    if (document == null || !document.isObject()) {
      throw invalid("DOCUMENT_REQUIRED", "Rich text document must be an object.");
    }
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
  }

  private void validateNode(JsonNode node, int depth, Counter counter, String parentType) {
    if (depth > limits.maxDepth()) {
      throw invalid("DEPTH_EXCEEDED", "Rich text nesting is too deep.");
    }
    if (!node.isObject()) {
      throw invalid("NODE_OBJECT", "Every rich text node must be an object.");
    }

    String type = requiredText(node, "type");
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
      case "paragraph" -> validateInlineChildren(node, depth, counter);
      case "heading" -> {
        int level = node.path("attrs").path("level").asInt();
        if (level < 2 || level > 4) {
          throw invalid("HEADING_LEVEL", "Heading level must be between 2 and 4.");
        }
        validateInlineChildren(node, depth, counter);
      }
      case "bulletList", "orderedList" -> {
        JsonNode children = requiredArray(node, "content");
        for (JsonNode child : children) {
          validateNode(child, depth + 1, counter, type);
        }
      }
      case "blockquote" -> validateChildren(requiredArray(node, "content"), depth + 1, counter);
      case "codeBlock" -> {
        JsonNode children = node.get("content");
        if (children != null) {
          for (JsonNode child : children) {
            String childType = requiredText(child, "type");
            if (!"text".equals(childType)) {
              throw invalid("CODE_BLOCK_CHILD", "Code blocks may only contain text.");
            }
            addText(counter, child.path("text").asText(""));
          }
        }
      }
      case "horizontalRule" -> {}
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
            String href = requiredText(mark.path("attrs"), "href");
            validateHttpUrl(href);
          }
        }
      }
    }
  }

  private void validateImage(JsonNode node, Counter counter) {
    JsonNode attrs = requiredObject(node, "attrs");
    requireOnlyAttributes(attrs, IMAGE_ATTRS);
    UUID.fromString(requiredText(attrs, "assetId"));
    if (attrs.hasNonNull("uploadId")) {
      UUID.fromString(requiredText(attrs, "uploadId"));
    }
    requiredText(attrs, "nodeId");
    int width = attrs.path("displayWidthPx").asInt();
    if (width < 80 || width > limits.maxDisplayWidth()) {
      throw invalid("IMAGE_WIDTH", "Image display width is outside the supported range.");
    }
    double aspectRatio = attrs.path("aspectRatio").asDouble();
    if (aspectRatio < 0.01 || aspectRatio > 100) {
      throw invalid("IMAGE_ASPECT", "Image aspect ratio is invalid.");
    }
    if (!ALIGNMENTS.contains(attrs.path("alignment").asText())) {
      throw invalid("IMAGE_ALIGN", "Image alignment is invalid.");
    }
    boolean decorative = attrs.path("decorative").asBoolean(false);
    if (!decorative && attrs.path("alt").asText("").isBlank()) {
      throw invalid("IMAGE_ALT_REQUIRED", "Non-decorative images require alt text.");
    }
    checkMaxLength(attrs.path("alt"), 500, "IMAGE_ALT_LENGTH");
    checkMaxLength(attrs.path("caption"), 500, "IMAGE_CAPTION_LENGTH");
    counter.images++;
  }

  private void validateTable(JsonNode node, int depth, Counter counter) {
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
      URI uri = URI.create(raw);
      if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null) {
        throw new IllegalArgumentException("unsupported");
      }
    } catch (IllegalArgumentException error) {
      throw invalid("UNSAFE_LINK", "Links must use an absolute HTTP or HTTPS URL.");
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
  }
}

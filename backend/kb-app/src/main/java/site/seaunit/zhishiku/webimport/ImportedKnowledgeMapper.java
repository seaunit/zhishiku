package site.seaunit.zhishiku.webimport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;
import site.seaunit.zhishiku.richtext.image.ImportedImageAsset;

@Component
public class ImportedKnowledgeMapper {
  private static final Pattern HEADING = Pattern.compile("^(#{1,4})\\s+(.+)$");
  private static final Pattern BULLET = Pattern.compile("^[-*]\\s+(.+)$");
  private static final Pattern ORDERED = Pattern.compile("^\\d+[.)]\\s+(.+)$");
  private static final Pattern TABLE_SEPARATOR =
      Pattern.compile("^\\s*\\|?\\s*:?-{3,}:?\\s*(?:\\|\\s*:?-{3,}:?\\s*)+\\|?\\s*$");
  private static final Pattern INLINE =
      Pattern.compile("(\\*\\*[^*]+\\*\\*|`[^`]+`|\\[[^\\]]+\\]\\(https?://[^)]+\\))");
  private final ObjectMapper mapper;

  public ImportedKnowledgeMapper(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public ObjectNode document(String body) {
    return document(body, List.of());
  }

  public ObjectNode document(String body, List<ImportedImageAsset> images) {
    ObjectNode document = mapper.createObjectNode();
    document.put("type", "doc");
    ArrayNode content = document.putArray("content");
    String normalized = body.replace("\r\n", "\n").strip();
    String[] lines = normalized.split("\n", -1);
    int index = 0;
    while (index < lines.length) {
      String line = lines[index].strip();
      if (line.isBlank()) {
        index++;
        continue;
      }
      if (line.startsWith("```")) {
        String language = line.substring(3).strip();
        ObjectNode code = content.addObject().put("type", "codeBlock");
        if (!language.isBlank()) {
          code.putObject("attrs").put("language", safeLanguage(language));
        }
        ArrayNode codeContent = code.putArray("content");
        index++;
        StringBuilder text = new StringBuilder();
        while (index < lines.length && !lines[index].strip().startsWith("```")) {
          if (!text.isEmpty()) {
            text.append('\n');
          }
          text.append(lines[index]);
          index++;
        }
        codeContent.addObject().put("type", "text").put("text", text.toString());
        if (index < lines.length) {
          index++;
        }
        continue;
      }
      Matcher heading = HEADING.matcher(line);
      if (heading.matches()) {
        int level = Math.min(4, Math.max(2, heading.group(1).length()));
        ObjectNode node = content.addObject().put("type", "heading");
        node.putObject("attrs").put("level", level);
        appendInline(node.putArray("content"), heading.group(2));
        index++;
        continue;
      }
      if (BULLET.matcher(line).matches()) {
        ObjectNode list = content.addObject().put("type", "bulletList");
        ArrayNode items = list.putArray("content");
        while (index < lines.length) {
          Matcher bullet = BULLET.matcher(lines[index].strip());
          if (!bullet.matches()) {
            break;
          }
          appendListItem(items, bullet.group(1));
          index++;
        }
        continue;
      }
      Matcher ordered = ORDERED.matcher(line);
      if (ordered.matches()) {
        ObjectNode list = content.addObject().put("type", "orderedList");
        ArrayNode items = list.putArray("content");
        while (index < lines.length) {
          Matcher item = ORDERED.matcher(lines[index].strip());
          if (!item.matches()) {
            break;
          }
          appendListItem(items, item.group(1));
          index++;
        }
        continue;
      }
      if (line.startsWith(">")) {
        ObjectNode quote = content.addObject().put("type", "blockquote");
        StringBuilder quoteText = new StringBuilder();
        while (index < lines.length && lines[index].strip().startsWith(">")) {
          String quoted = lines[index].strip().substring(1).strip();
          if (!quoteText.isEmpty()) {
            quoteText.append(' ');
          }
          quoteText.append(quoted);
          index++;
        }
        appendInline(
            quote.putArray("content").addObject().put("type", "paragraph").putArray("content"),
            quoteText.toString());
        continue;
      }
      if (line.matches("^[-*_]{3,}$")) {
        content.addObject().put("type", "horizontalRule");
        index++;
        continue;
      }
      if (isTableStart(lines, index)) {
        index = appendTable(content, lines, index);
        continue;
      }
      StringBuilder paragraph = new StringBuilder();
      while (index < lines.length) {
        String candidate = lines[index].strip();
        if (candidate.isBlank()
            || candidate.startsWith("```")
            || candidate.startsWith(">")
            || HEADING.matcher(candidate).matches()
            || BULLET.matcher(candidate).matches()
            || ORDERED.matcher(candidate).matches()
            || candidate.matches("^[-*_]{3,}$")
            || isTableStart(lines, index)) {
          break;
        }
        if (!paragraph.isEmpty()) {
          paragraph.append(' ');
        }
        paragraph.append(candidate);
        index++;
      }
      ObjectNode paragraphNode = content.addObject().put("type", "paragraph");
      appendInline(paragraphNode.putArray("content"), paragraph.toString());
    }
    if (content.isEmpty()) {
      content.addObject().put("type", "paragraph");
    }
    appendImages(content, images == null ? List.of() : images);
    return document;
  }

  private static int appendTable(ArrayNode content, String[] lines, int start) {
    List<String> headers = splitTableRow(lines[start]);
    ObjectNode table = content.addObject().put("type", "table");
    table.putObject("attrs").put("headerRow", true);
    ArrayNode rows = table.putArray("content");
    appendTableRow(rows, headers, "tableHeaderRow", "tableHeader");
    int index = start + 2;
    while (index < lines.length && !lines[index].isBlank() && lines[index].contains("|")) {
      List<String> cells = splitTableRow(lines[index]);
      appendTableRow(rows, cells, "tableBodyRow", "tableCell");
      index++;
    }
    return index;
  }

  private static void appendTableRow(
      ArrayNode rows, List<String> values, String rowType, String cellType) {
    ObjectNode row = rows.addObject().put("type", rowType);
    ArrayNode cells = row.putArray("content");
    for (String value : values) {
      ObjectNode paragraph =
          cells
              .addObject()
              .put("type", cellType)
              .putArray("content")
              .addObject()
              .put("type", "paragraph");
      if (!value.isBlank()) {
        appendInline(paragraph.putArray("content"), value);
      }
    }
  }

  private static List<String> splitTableRow(String line) {
    String value = line.strip();
    if (value.startsWith("|")) {
      value = value.substring(1);
    }
    if (value.endsWith("|")) {
      value = value.substring(0, value.length() - 1);
    }
    String[] cells = value.split("\\|", -1);
    List<String> result = new ArrayList<>(cells.length);
    for (String cell : cells) {
      result.add(cell.strip());
    }
    return result;
  }

  private static boolean isTableStart(String[] lines, int index) {
    return index + 1 < lines.length
        && lines[index].contains("|")
        && TABLE_SEPARATOR.matcher(lines[index + 1].strip()).matches();
  }

  private static void appendImages(ArrayNode content, List<ImportedImageAsset> images) {
    if (images.isEmpty()) {
      return;
    }
    ObjectNode heading = content.addObject().put("type", "heading");
    heading.putObject("attrs").put("level", 2);
    heading.putArray("content").addObject().put("type", "text").put("text", "来源图示");
    int index = 0;
    for (ImportedImageAsset image : images) {
      ObjectNode node = content.addObject().put("type", "image");
      ObjectNode attrs = node.putObject("attrs");
      attrs.put("assetId", image.uploadId().toString());
      attrs.put("nodeId", "import-image-" + (++index) + "-" + image.uploadId());
      attrs.put("alt", image.alt());
      attrs.put("caption", image.caption());
      attrs.put("displayWidthPx", Math.min(1200, Math.max(80, image.width())));
      attrs.put("aspectRatio", Math.max(0.01, Math.min(100, image.aspectRatio())));
      attrs.put("alignment", "center");
      attrs.put("decorative", false);
    }
  }

  private static void appendListItem(ArrayNode items, String text) {
    ObjectNode item = items.addObject().put("type", "listItem");
    ObjectNode paragraph = item.putArray("content").addObject().put("type", "paragraph");
    appendInline(paragraph.putArray("content"), text);
  }

  private static void appendInline(ArrayNode content, String value) {
    Matcher matcher = INLINE.matcher(value);
    int cursor = 0;
    while (matcher.find()) {
      if (matcher.start() > cursor) {
        content
            .addObject()
            .put("type", "text")
            .put("text", value.substring(cursor, matcher.start()));
      }
      String token = matcher.group();
      if (token.startsWith("**") && token.endsWith("**")) {
        content
            .addObject()
            .put("type", "text")
            .put("text", token.substring(2, token.length() - 2))
            .putArray("marks")
            .addObject()
            .put("type", "bold");
      } else if (token.startsWith("`") && token.endsWith("`")) {
        content
            .addObject()
            .put("type", "text")
            .put("text", token.substring(1, token.length() - 1))
            .putArray("marks")
            .addObject()
            .put("type", "code");
      } else {
        int split = token.indexOf("](");
        String label = token.substring(1, split);
        String href = token.substring(split + 2, token.length() - 1);
        content
            .addObject()
            .put("type", "text")
            .put("text", label)
            .putArray("marks")
            .addObject()
            .put("type", "link")
            .putObject("attrs")
            .put("href", href);
      }
      cursor = matcher.end();
    }
    if (cursor < value.length()) {
      content.addObject().put("type", "text").put("text", value.substring(cursor));
    }
  }

  private static String safeLanguage(String value) {
    return value.replaceAll("[^A-Za-z0-9_+-]", "");
  }

  public String note(
      OrganizedKnowledge note, boolean redactionApplied, String userNote, String sourceFormat) {
    List<String> parts = new ArrayList<>();
    if (sourceFormat != null && !sourceFormat.isBlank()) {
      parts.add("来源格式：" + sourceFormat.strip());
    }
    if (userNote != null && !userNote.isBlank()) {
      parts.add("用户备注：" + userNote.strip());
    }
    if (!note.summary().isBlank()) {
      parts.add("AI 摘要：" + note.summary());
    }
    if (!note.category().isBlank()) {
      parts.add("AI 分类：" + note.category());
    }
    if (!note.tags().isEmpty()) {
      parts.add("AI 标签：" + String.join("、", note.tags()));
    }
    if (redactionApplied) {
      parts.add("安全处理：已移除疑似密钥");
    }
    String noteText = String.join("\n", parts);
    return noteText.substring(0, Math.min(noteText.length(), 20_000));
  }

  public KnowledgeItemResponse withRedactionApplied(
      KnowledgeItemResponse item, boolean redactionApplied) {
    if (!redactionApplied || item.redactionApplied()) {
      return item;
    }
    return new KnowledgeItemResponse(
        item.id(),
        item.title(),
        item.body(),
        item.bodyDocument(),
        item.bodySchemaVersion(),
        item.bodyMigrationState(),
        item.note(),
        item.summary(),
        item.processingStatus(),
        item.lifecycleState(),
        true,
        item.source(),
        item.category(),
        item.tags(),
        item.version(),
        item.createdAt(),
        item.updatedAt());
  }
}

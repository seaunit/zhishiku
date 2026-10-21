package site.seaunit.zhishiku.core.richtext;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

public final class PlainTextProjector {
  public String project(JsonNode document) {
    List<String> blocks = new ArrayList<>();
    JsonNode content = document.path("content");
    if (content.isArray()) {
      for (JsonNode node : content) {
        String text = blockText(node);
        if (!text.isBlank()) {
          blocks.add(text.strip());
        }
      }
    }
    return String.join("\n\n", blocks)
        .replaceAll("[\\t ]+", " ")
        .replaceAll("\\n{3,}", "\n\n")
        .strip();
  }

  private String blockText(JsonNode node) {
    if (node == null || !node.isObject()) {
      return "";
    }
    return switch (node.path("type").asText()) {
      case "text" -> node.path("text").asText("");
      case "hardBreak" -> "\n";
      case "image" -> imageText(node.path("attrs"));
      case "codeBlock" -> inlineContent(node.path("content"));
      case "bulletList", "orderedList" -> listText(node);
      case "table" -> tableText(node);
      case "horizontalRule" -> "";
      default -> inlineContent(node.path("content"));
    };
  }

  private String listText(JsonNode node) {
    List<String> items = new ArrayList<>();
    for (JsonNode item : node.path("content")) {
      String text = inlineContent(item.path("content"));
      if (!text.isBlank()) {
        items.add("- " + text.strip());
      }
    }
    return String.join("\n", items);
  }

  private String tableText(JsonNode table) {
    List<String> rows = new ArrayList<>();
    for (JsonNode row : table.path("content")) {
      List<String> cells = new ArrayList<>();
      for (JsonNode cell : row.path("content")) {
        cells.add(inlineContent(cell.path("content")).strip());
      }
      rows.add(String.join(" | ", cells));
    }
    return String.join("\n", rows);
  }

  private String imageText(JsonNode attrs) {
    String caption = attrs.path("caption").asText("").strip();
    String alt = attrs.path("alt").asText("").strip();
    if (!caption.isBlank() && !alt.isBlank() && !caption.equals(alt)) {
      return caption + " " + alt;
    }
    return !caption.isBlank() ? caption : alt;
  }

  private String inlineContent(JsonNode content) {
    List<String> parts = new ArrayList<>();
    if (content.isArray()) {
      for (JsonNode child : content) {
        parts.add(blockText(child));
      }
    } else if (content.isObject()) {
      parts.add(blockText(content));
    }
    return String.join("", parts);
  }
}

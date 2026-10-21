package site.seaunit.zhishiku.core.richtext.paste;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import site.seaunit.zhishiku.core.richtext.RichTextUrlPolicy;

/**
 * Converts untrusted clipboard HTML into the supported structured rich-text subset.
 *
 * <p>The converter never fetches remote resources. Unsupported and dangerous elements are removed
 * before the document is returned, and URLs use the same public HTTP(S) policy as server
 * validation.
 */
public final class HtmlToRichTextConverter {
  private static final int MAX_TABLE_ROWS = 50;
  private static final int MAX_TABLE_COLUMNS = 20;
  private static final Set<String> BLOCKED_ELEMENTS =
      Set.of(
          "script", "style", "iframe", "form", "input", "button", "object", "embed", "svg",
          "canvas", "video", "audio", "link", "meta", "base");

  private final ObjectMapper mapper;

  public HtmlToRichTextConverter(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public Conversion convert(String html, String plainTextFallback) {
    Document parsed = Jsoup.parse(html == null ? "" : html);
    ConversionContext context = new ConversionContext();
    ArrayNode blocks = mapper.createArrayNode();
    appendBlocks(parsed.body().childNodes(), blocks, context);

    if (blocks.isEmpty()) {
      appendPlainText(plainTextFallback, blocks, context);
    }
    if (blocks.isEmpty()) {
      blocks.add(mapper.createObjectNode().put("type", "paragraph"));
    }

    ObjectNode document = mapper.createObjectNode();
    document.put("type", "doc");
    document.set("content", blocks);
    return new Conversion(
        document,
        List.copyOf(context.preserved),
        List.copyOf(context.downgraded),
        List.copyOf(context.removed),
        context.imagePlaceholders);
  }

  private void appendBlocks(List<Node> nodes, ArrayNode output, ConversionContext context) {
    for (Node node : nodes) {
      if (node instanceof TextNode textNode) {
        String text = textNode.text().strip();
        if (!text.isBlank()) {
          appendParagraph(output, List.of(textNode(text)));
        }
        continue;
      }
      if (!(node instanceof Element element)) {
        continue;
      }

      String tag = tag(element);
      if (BLOCKED_ELEMENTS.contains(tag)) {
        context.removed.add(tag);
        continue;
      }
      if (isHeading(tag)) {
        appendHeading(element, tag, output, context);
      } else if ("p".equals(tag) || "div".equals(tag) || "section".equals(tag)) {
        context.preserved.add("paragraph");
        appendParagraph(output, inlineNodes(element.childNodes(), context));
      } else if ("ul".equals(tag) || "ol".equals(tag)) {
        appendList(element, tag, output, context);
      } else if ("blockquote".equals(tag)) {
        context.preserved.add("quote");
        ObjectNode quote = mapper.createObjectNode().put("type", "blockquote");
        ArrayNode quoteBlocks = mapper.createArrayNode();
        appendParagraph(quoteBlocks, inlineNodes(element.childNodes(), context));
        quote.set("content", quoteBlocks);
        output.add(quote);
      } else if ("pre".equals(tag)) {
        context.preserved.add("code");
        ObjectNode code = mapper.createObjectNode().put("type", "codeBlock");
        ArrayNode text = mapper.createArrayNode();
        text.add(mapper.createObjectNode().put("type", "text").put("text", element.text()));
        code.set("content", text);
        output.add(code);
      } else if ("hr".equals(tag)) {
        output.add(mapper.createObjectNode().put("type", "horizontalRule"));
      } else if ("table".equals(tag)) {
        appendTable(element, output, context);
      } else if ("img".equals(tag)) {
        appendImagePlaceholder(element, output, context);
      } else {
        ArrayNode nested = mapper.createArrayNode();
        appendBlocks(element.childNodes(), nested, context);
        if (!nested.isEmpty()) {
          nested.forEach(output::add);
        } else {
          List<ObjectNode> inline = inlineNodes(element.childNodes(), context);
          if (!inline.isEmpty()) {
            appendParagraph(output, inline);
          }
        }
      }
    }
  }

  private void appendHeading(
      Element element, String tag, ArrayNode output, ConversionContext context) {
    int sourceLevel = Integer.parseInt(tag.substring(1));
    int level = Math.max(2, Math.min(4, sourceLevel));
    if (sourceLevel == level) {
      context.preserved.add("heading");
    } else {
      context.downgraded.add("heading");
    }
    ObjectNode heading = mapper.createObjectNode().put("type", "heading");
    heading.set("attrs", mapper.createObjectNode().put("level", level));
    heading.set(
        "content", mapper.createArrayNode().addAll(inlineNodes(element.childNodes(), context)));
    output.add(heading);
  }

  private void appendParagraph(ArrayNode output, List<ObjectNode> inline) {
    ObjectNode paragraph = mapper.createObjectNode().put("type", "paragraph");
    ArrayNode content = mapper.createArrayNode();
    inline.forEach(content::add);
    if (!content.isEmpty()) {
      paragraph.set("content", content);
    }
    output.add(paragraph);
  }

  private void appendList(
      Element element, String tag, ArrayNode output, ConversionContext context) {
    context.preserved.add("list");
    ObjectNode list =
        mapper.createObjectNode().put("type", tag.equals("ol") ? "orderedList" : "bulletList");
    ArrayNode items = mapper.createArrayNode();
    element.children().stream()
        .filter(child -> "li".equals(tag(child)))
        .forEach(
            item -> {
              ObjectNode listItem = mapper.createObjectNode().put("type", "listItem");
              ArrayNode itemBlocks = mapper.createArrayNode();
              appendParagraph(itemBlocks, inlineNodes(item.childNodes(), context));
              listItem.set("content", itemBlocks);
              items.add(listItem);
            });
    if (!items.isEmpty()) {
      list.set("content", items);
      output.add(list);
    }
  }

  private void appendTable(Element element, ArrayNode output, ConversionContext context) {
    List<Element> sourceRows = element.select("tr");
    if (sourceRows.isEmpty()) {
      return;
    }
    context.preserved.add("table");
    ObjectNode table = mapper.createObjectNode().put("type", "table");
    table.set("attrs", mapper.createObjectNode().put("headerRow", true));
    ArrayNode rows = mapper.createArrayNode();
    int rowLimit = Math.min(sourceRows.size(), MAX_TABLE_ROWS);
    for (int rowIndex = 0; rowIndex < rowLimit; rowIndex++) {
      Element sourceRow = sourceRows.get(rowIndex);
      List<Element> cells =
          sourceRow.children().stream().filter(cell -> isTableCell(tag(cell))).toList();
      int columnLimit = Math.min(cells.size(), MAX_TABLE_COLUMNS);
      if (columnLimit == 0) {
        continue;
      }
      ObjectNode row =
          mapper.createObjectNode().put("type", rowIndex == 0 ? "tableHeaderRow" : "tableBodyRow");
      ArrayNode rowCells = mapper.createArrayNode();
      for (int columnIndex = 0; columnIndex < columnLimit; columnIndex++) {
        Element sourceCell = cells.get(columnIndex);
        boolean header = rowIndex == 0;
        if (header && !"th".equals(tag(sourceCell))) {
          context.downgraded.add("table");
        }
        ObjectNode cell =
            mapper.createObjectNode().put("type", header ? "tableHeader" : "tableCell");
        ArrayNode paragraphs = mapper.createArrayNode();
        appendParagraph(paragraphs, inlineNodes(sourceCell.childNodes(), context));
        cell.set("content", paragraphs);
        rowCells.add(cell);
      }
      row.set("content", rowCells);
      rows.add(row);
    }
    if (!rows.isEmpty()) {
      table.set("content", rows);
      output.add(table);
    }
  }

  private void appendImagePlaceholder(Element image, ArrayNode output, ConversionContext context) {
    context.removed.add("remote-image");
    appendParagraph(output, List.of(imagePlaceholderText(image, context)));
  }

  private ObjectNode imagePlaceholderText(Element image, ConversionContext context) {
    String alt = image.attr("alt").strip();
    String label = alt.isBlank() ? "图片未导入" : "图片未导入：" + alt;
    ObjectNode text = textNode(label);
    String source = safeUrl(image.attr("src"));
    if (source != null) {
      context.downgraded.add("image");
      ObjectNode link = mapper.createObjectNode().put("type", "link");
      link.set("attrs", mapper.createObjectNode().put("href", source));
      text.set("marks", mapper.createArrayNode().add(link));
    }
    context.imagePlaceholders++;
    return text;
  }

  private List<ObjectNode> inlineNodes(List<Node> nodes, ConversionContext context) {
    List<ObjectNode> output = new ArrayList<>();
    for (Node node : nodes) {
      if (node instanceof TextNode textNode) {
        if (!textNode.text().isEmpty()) {
          output.add(textNode(textNode.text()));
        }
        continue;
      }
      if (!(node instanceof Element element)) {
        continue;
      }
      String tag = tag(element);
      if (BLOCKED_ELEMENTS.contains(tag)) {
        context.removed.add(tag);
        continue;
      }
      if ("br".equals(tag)) {
        output.add(mapper.createObjectNode().put("type", "hardBreak"));
        continue;
      }
      if ("img".equals(tag)) {
        context.removed.add("remote-image");
        output.add(imagePlaceholderText(element, context));
        continue;
      }

      List<ObjectNode> marks = marksFor(element, context);
      List<ObjectNode> children = inlineNodes(element.childNodes(), context);
      for (ObjectNode child : children) {
        if ("text".equals(child.path("type").asText()) && !marks.isEmpty()) {
          ArrayNode existing =
              child.has("marks") ? (ArrayNode) child.get("marks") : mapper.createArrayNode();
          Set<String> seen = new LinkedHashSet<>();
          existing.forEach(mark -> seen.add(mark.path("type").asText()));
          for (ObjectNode mark : marks) {
            if (seen.add(mark.path("type").asText()) && existing.size() < 6) {
              existing.add(mark);
            }
          }
          child.set("marks", existing);
        }
        output.add(child);
      }
    }
    return output;
  }

  private List<ObjectNode> marksFor(Element element, ConversionContext context) {
    String tag = tag(element);
    String style = element.attr("style").toLowerCase(Locale.ROOT);
    List<ObjectNode> marks = new ArrayList<>();
    if ("strong".equals(tag)
        || "b".equals(tag)
        || style.matches(".*font-weight\\s*:\\s*(bold|[6-9]00).*")) {
      marks.add(simpleMark("bold"));
      context.preserved.add("bold");
    }
    if ("em".equals(tag) || "i".equals(tag) || style.contains("font-style:italic")) {
      marks.add(simpleMark("italic"));
      context.preserved.add("italic");
    }
    if ("u".equals(tag) || style.matches(".*text-decoration[^;]*underline.*")) {
      marks.add(simpleMark("underline"));
      context.preserved.add("underline");
    }
    if ("s".equals(tag) || "strike".equals(tag) || style.contains("line-through")) {
      marks.add(simpleMark("strike"));
      context.preserved.add("strike");
    }
    if ("code".equals(tag)) {
      marks.add(simpleMark("code"));
      context.preserved.add("code");
    }
    if ("a".equals(tag)) {
      String href = safeUrl(element.attr("href"));
      if (href != null) {
        ObjectNode link = mapper.createObjectNode().put("type", "link");
        link.set("attrs", mapper.createObjectNode().put("href", href));
        marks.add(link);
        context.preserved.add("link");
      } else if (element.hasAttr("href")) {
        context.removed.add("unsafe-link");
      }
    }
    return marks;
  }

  private void appendPlainText(String text, ArrayNode output, ConversionContext context) {
    String value = text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n').strip();
    if (value.isBlank()) {
      return;
    }
    context.downgraded.add("paragraph");
    for (String paragraph : value.split("\\n{2,}")) {
      List<ObjectNode> inline = new ArrayList<>();
      String[] lines = paragraph.split("\\n", -1);
      for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
        if (!lines[lineIndex].isEmpty()) {
          inline.add(textNode(lines[lineIndex]));
        }
        if (lineIndex < lines.length - 1) {
          inline.add(mapper.createObjectNode().put("type", "hardBreak"));
        }
      }
      appendParagraph(output, inline);
    }
  }

  private ObjectNode simpleMark(String type) {
    return mapper.createObjectNode().put("type", type);
  }

  private ObjectNode textNode(String text) {
    return mapper.createObjectNode().put("type", "text").put("text", text);
  }

  private static String safeUrl(String raw) {
    try {
      return RichTextUrlPolicy.validateAndNormalize(raw);
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private static boolean isHeading(String tag) {
    return tag.matches("h[1-6]");
  }

  private static boolean isTableCell(String tag) {
    return "th".equals(tag) || "td".equals(tag);
  }

  private static String tag(Element element) {
    return element.tagName().toLowerCase(Locale.ROOT);
  }

  public record Conversion(
      ObjectNode document,
      List<String> preservedFeatures,
      List<String> downgradedFeatures,
      List<String> removedFeatures,
      int imagePlaceholders) {}

  private final class ConversionContext {
    private final Set<String> preserved = new LinkedHashSet<>();
    private final Set<String> downgraded = new LinkedHashSet<>();
    private final Set<String> removed = new LinkedHashSet<>();
    private int imagePlaceholders;
  }
}

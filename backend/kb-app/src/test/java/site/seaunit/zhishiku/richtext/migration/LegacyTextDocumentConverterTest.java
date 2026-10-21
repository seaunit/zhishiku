package site.seaunit.zhishiku.richtext.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LegacyTextDocumentConverterTest {
  private final LegacyTextDocumentConverter converter = new LegacyTextDocumentConverter();

  @Test
  void preservesParagraphsLineBreaksAndDetectsHttpLinks() {
    String source = "第一段\n保留的换行\n\n第二段 https://example.com/path?q=1。\r\n结束";

    LegacyTextDocumentConverter.Conversion conversion = converter.convert(source);

    assertThat(conversion.plainText())
        .isEqualTo("第一段\n保留的换行\n\n第二段 https://example.com/path?q=1。\n结束");
    assertThat(conversion.document().path("type").asText()).isEqualTo("doc");
    assertThat(conversion.document().path("content")).hasSize(2);
    assertThat(findTextNodes(conversion.document()))
        .anySatisfy(
            node -> {
              assertThat(node.path("text").asText()).isEqualTo("https://example.com/path?q=1");
              assertThat(node.path("marks").get(0).path("type").asText()).isEqualTo("link");
            });
  }

  @Test
  void sameSourceProducesSameDocumentAndHash() {
    LegacyTextDocumentConverter.Conversion first = converter.convert("幂等\n迁移");
    LegacyTextDocumentConverter.Conversion second = converter.convert("幂等\r\n迁移");

    assertThat(second.document()).isEqualTo(first.document());
    assertThat(second.sourceHash()).isEqualTo(first.sourceHash()).hasSize(64);
    assertThat(second.plainText()).isEqualTo(first.plainText());
  }

  private static List<JsonNode> findTextNodes(JsonNode document) {
    List<JsonNode> nodes = new ArrayList<>();
    collectTextNodes(document, nodes);
    return nodes;
  }

  private static void collectTextNodes(JsonNode node, List<JsonNode> nodes) {
    if (node == null) {
      return;
    }
    if (node.isObject() && "text".equals(node.path("type").asText())) {
      nodes.add(node);
    }
    if (node.isContainerNode()) {
      node.forEach(child -> collectTextNodes(child, nodes));
    }
  }
}

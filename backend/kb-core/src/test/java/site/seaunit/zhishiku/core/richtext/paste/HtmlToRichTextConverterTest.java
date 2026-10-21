package site.seaunit.zhishiku.core.richtext.paste;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import site.seaunit.zhishiku.core.richtext.RichTextValidator;

class HtmlToRichTextConverterTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final HtmlToRichTextConverter converter = new HtmlToRichTextConverter(mapper);
  private final RichTextValidator validator = RichTextValidator.defaults();

  @Test
  void convertsWordAndGoogleDocsStyleFormattingWithoutRawHtml() {
    HtmlToRichTextConverter.Conversion conversion =
        converter.convert(
            """
            <h1>标题</h1>
            <p><strong>粗体</strong>、<em>斜体</em>、<u>下划线</u>、<s>删除线</s>、
            <code>code()</code> 和 <a href="https://example.com/doc">链接</a></p>
            <ul><li>项目一</li><li>项目二</li></ul>
            """,
            "fallback");

    JsonNode document = conversion.document();
    validator.validate(document);
    assertThat(document.toString()).doesNotContain("<strong>", "style=", "onclick=");
    assertThat(document.at("/content/0/type").asText()).isEqualTo("heading");
    assertThat(document.at("/content/0/attrs/level").asInt()).isEqualTo(2);
    assertThat(document.at("/content/2/type").asText()).isEqualTo("bulletList");
    assertThat(conversion.downgradedFeatures()).contains("heading");
    assertThat(conversion.preservedFeatures())
        .contains("paragraph", "bold", "italic", "underline", "strike", "code", "link", "list");
  }

  @Test
  void removesScriptsFormsIframesAndUnsafeLinks() {
    HtmlToRichTextConverter.Conversion conversion =
        converter.convert(
            """
            <p>保留文字<script>alert(1)</script><iframe src="https://evil.example"></iframe></p>
            <form><input value="secret"><button>提交</button></form>
            <p><a href="javascript:alert(1)">危险链接</a></p>
            <p><a href="https://user:pass@example.com/private">凭据链接</a></p>
            """,
            "");

    JsonNode document = conversion.document();
    validator.validate(document);
    String serialized = document.toString();
    assertThat(serialized).contains("保留文字", "危险链接", "凭据链接");
    assertThat(serialized).doesNotContain("alert(1)", "javascript:", "user:pass", "<iframe");
    assertThat(conversion.removedFeatures()).contains("script", "iframe", "form");
    assertThat(conversion.removedFeatures()).contains("unsafe-link");
  }

  @Test
  void convertsTablesWithAHeaderRowAndDropsRemoteImagesIntoSafePlaceholders() {
    HtmlToRichTextConverter.Conversion conversion =
        converter.convert(
            """
            <table>
              <tr><td>名称</td><td>说明</td></tr>
              <tr><td>识海</td><td>个人知识库</td></tr>
            </table>
            <p><img src="https://tracker.example/pixel.png" alt="示意图"></p>
            """,
            "fallback");

    JsonNode document = conversion.document();
    validator.validate(document);
    assertThat(document.at("/content/0/type").asText()).isEqualTo("table");
    assertThat(document.at("/content/0/content/0/type").asText()).isEqualTo("tableHeaderRow");
    assertThat(document.at("/content/0/content/1/type").asText()).isEqualTo("tableBodyRow");
    assertThat(document.toString()).contains("图片未导入：示意图");
    assertThat(conversion.imagePlaceholders()).isEqualTo(1);
    assertThat(conversion.removedFeatures()).contains("remote-image");
  }

  @Test
  void fallsBackToPlainTextWithParagraphsAndHardBreaks() {
    HtmlToRichTextConverter.Conversion conversion =
        converter.convert("<script>alert(1)</script>", "第一段\n第二行\n\n第二段");

    JsonNode document = conversion.document();
    validator.validate(document);
    assertThat(document.at("/content/0/content/1/type").asText()).isEqualTo("hardBreak");
    assertThat(document.at("/content/1/content/0/text").asText()).isEqualTo("第二段");
  }
}

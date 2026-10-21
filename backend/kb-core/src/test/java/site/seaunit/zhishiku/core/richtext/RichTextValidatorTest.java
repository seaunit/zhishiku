package site.seaunit.zhishiku.core.richtext;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class RichTextValidatorTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final RichTextValidator validator = RichTextValidator.defaults();

  @Test
  void acceptsSupportedDocument() throws Exception {
    var document =
        mapper.readTree(
            """
            {"type":"doc","content":[
              {"type":"heading","attrs":{"level":2},"content":[
                {"type":"text","text":"知识标题","marks":[{"type":"bold"}]}
              ]},
              {"type":"paragraph","content":[
                {"type":"text","text":"正文"},
                {"type":"hardBreak"},
                {"type":"text","text":"链接","marks":[
                  {"type":"link","attrs":{"href":"https://example.com"}}
                ]}
              ]},
              {"type":"image","attrs":{
                "assetId":"11111111-1111-1111-1111-111111111111",
                "nodeId":"img-1","alt":"示意图","caption":"图一",
                "displayWidthPx":720,"aspectRatio":1.5,
                "alignment":"center","decorative":false
              }}
            ]}
            """);

    assertThatCode(() -> validator.validate(document)).doesNotThrowAnyException();
  }

  @Test
  void rejectsUnknownNodeAndRemoteImageSource() throws Exception {
    var unknown = mapper.readTree("{\"type\":\"doc\",\"content\":[{\"type\":\"script\"}]}");
    assertThatThrownBy(() -> validator.validate(unknown))
        .isInstanceOf(RichTextException.class)
        .hasMessageContaining("Unsupported");

    var remoteImage =
        mapper.readTree(
            """
            {"type":"doc","content":[{"type":"image","attrs":{
              "assetId":"11111111-1111-1111-1111-111111111111",
              "nodeId":"img-1","alt":"x","displayWidthPx":320,
              "aspectRatio":1,"alignment":"left","decorative":false,
              "src":"https://tracker.example/pixel.gif"
            }}]}
            """);
    assertThatThrownBy(() -> validator.validate(remoteImage))
        .isInstanceOf(RichTextException.class)
        .hasMessageContaining("attribute");
  }

  @Test
  void rejectsInvalidTableOrder() throws Exception {
    var table =
        mapper.readTree(
            """
            {"type":"doc","content":[{"type":"table","attrs":{"headerRow":true},"content":[
              {"type":"tableBodyRow","content":[
                {"type":"tableCell","content":[{"type":"paragraph","content":[
                  {"type":"text","text":"data"}
                ]}]}
              ]}
            ]}]}
            """);

    assertThatThrownBy(() -> validator.validate(table))
        .isInstanceOf(RichTextException.class)
        .hasMessageContaining("First row");
  }
}

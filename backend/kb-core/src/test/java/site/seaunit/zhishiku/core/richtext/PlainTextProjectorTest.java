package site.seaunit.zhishiku.core.richtext;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PlainTextProjectorTest {
  @Test
  void projectsVisibleTextAndImageMetadata() throws Exception {
    var document =
        new ObjectMapper()
            .readTree(
                """
                {"type":"doc","content":[
                  {"type":"heading","attrs":{"level":2},"content":[
                    {"type":"text","text":"标题"}
                  ]},
                  {"type":"bulletList","content":[
                    {"type":"listItem","content":[
                      {"type":"paragraph","content":[{"type":"text","text":"第一项"}]}
                    ]}
                  ]},
                  {"type":"image","attrs":{
                    "assetId":"11111111-1111-1111-1111-111111111111",
                    "nodeId":"img-1","alt":"替代文本","caption":"图片说明",
                    "displayWidthPx":720,"aspectRatio":1.5,
                    "alignment":"center","decorative":false
                  }}
                ]}
                """);

    String text = new PlainTextProjector().project(document);

    assertThat(text).contains("标题", "- 第一项", "图片说明", "替代文本");
  }
}

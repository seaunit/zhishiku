package site.seaunit.zhishiku.search.richtext;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class SearchProjectionBuilderTest {
  private final SearchProjectionBuilder builder = new SearchProjectionBuilder(new ObjectMapper());

  @Test
  void indexesVisibleBodyTableCodeCaptionAndAltText() throws Exception {
    var document =
        new ObjectMapper()
            .readTree(
                """
                {"type":"doc","content":[
                  {"type":"paragraph","content":[{"type":"text","text":"正文检索词"}]},
                  {"type":"codeBlock","content":[{"type":"text","text":"code-search-token"}]},
                  {"type":"table","attrs":{"headerRow":true},"content":[
                    {"type":"tableHeaderRow","content":[
                      {"type":"tableHeader","content":[
                        {"type":"paragraph","content":[{"type":"text","text":"列标题"}]}
                      ]}
                    ]}
                  ]},
                  {"type":"image","attrs":{
                    "assetId":"11111111-1111-1111-1111-111111111111",
                    "nodeId":"img-1","alt":"替代文本检索","caption":"图片说明检索",
                    "displayWidthPx":720,"aspectRatio":1.5,
                    "alignment":"center","decorative":false
                  }}
                ]}
                """);

    SearchProjection projection = builder.project("标题检索词", document, "备注检索词");

    assertThat(projection.searchText())
        .contains("标题检索词", "正文检索词", "code-search-token", "列标题", "图片说明检索", "替代文本检索");
  }

  @Test
  void neverIndexesInternalNodeNamesAttributesOrStorageKeys() throws Exception {
    var document =
        new ObjectMapper()
            .readTree(
                """
                {"type":"doc","content":[
                  {"type":"paragraph","content":[{"type":"text","text":"只保留可见文字"}]},
                  {"type":"image","attrs":{
                    "assetId":"11111111-1111-1111-1111-111111111111",
                    "nodeId":"internal-node-secret","storageKey":"private/object/key",
                    "alt":"安全图片","caption":"",
                    "displayWidthPx":320,"aspectRatio":1,
                    "alignment":"left","decorative":false
                  }}
                ]}
                """);

    SearchProjection projection = builder.project("标题", document, "");

    assertThat(projection.searchText())
        .contains("只保留可见文字", "安全图片")
        .doesNotContain("internal-node-secret", "private/object/key", "assetId", "paragraph");
  }
}

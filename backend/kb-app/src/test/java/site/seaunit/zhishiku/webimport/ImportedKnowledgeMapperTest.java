package site.seaunit.zhishiku.webimport;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import site.seaunit.zhishiku.richtext.image.ImportedImageAsset;

class ImportedKnowledgeMapperTest {
  private final ImportedKnowledgeMapper mapper = new ImportedKnowledgeMapper(new ObjectMapper());

  @Test
  void convertsMarkdownSubsetIntoStructuredRichTextAndFigures() {
    UUID imageId = UUID.randomUUID();
    JsonNode document =
        mapper.document(
            """
            ## 核心概念

            这是一段包含 **重点结论** 和 [参考链接](https://example.com/reference) 的正文。

            - 第一条依据
            - 第二条依据

            1. 先观察
            2. 再验证

            > 关键引用

            ```java
            int answer = 42;
            ```
            """,
            List.of(new ImportedImageAsset(imageId, "架构图", "来源图示 · 架构图", 1200, 800, 1.5)));

    assertThat(document.path("content").findValuesAsText("type"))
        .contains("heading", "paragraph", "bulletList", "orderedList", "blockquote", "codeBlock");
    assertThat(document.path("content").toString())
        .contains("\"type\":\"bold\"")
        .contains("\"type\":\"link\"")
        .contains("来源图示");
    JsonNode image = document.path("content").get(document.path("content").size() - 1);
    assertThat(image.path("type").asText()).isEqualTo("image");
    assertThat(image.path("attrs").path("assetId").asText()).isEqualTo(imageId.toString());
    assertThat(image.path("attrs").path("alt").asText()).isEqualTo("架构图");
    assertThat(image.path("attrs").path("alignment").asText()).isEqualTo("center");
  }

  @Test
  void convertsMarkdownPipeTablesIntoRichTextTables() {
    JsonNode document =
        mapper.document(
            """
            ## 参数说明

            | 参数 | 含义 |
            | --- | --- |
            | base_url | API 基础地址 |
            | **model** | 模型名称 |
            """);

    JsonNode table = document.path("content").get(1);
    assertThat(table.path("type").asText()).isEqualTo("table");
    assertThat(table.path("content").get(0).path("type").asText()).isEqualTo("tableHeaderRow");
    assertThat(table.path("content").get(1).path("type").asText()).isEqualTo("tableBodyRow");
    assertThat(table.path("content").get(0).toString()).contains("参数", "含义");
    assertThat(table.path("content").get(1).toString()).contains("base_url", "API 基础地址");
    assertThat(table.path("content").get(2).toString())
        .contains("\"type\":\"bold\"")
        .contains("模型名称");
  }
}

package site.seaunit.zhishiku.webimport;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import site.seaunit.zhishiku.config.AppProperties;

class DeepSeekKnowledgeOrganizerTest {
  @Test
  void sendsAStructuredSinglePagePromptAndParsesKnowledgeItems() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    var received = new LinkedHashMap<String, Object>();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/chat/completions",
        exchange -> {
          received.put("authorization", exchange.getRequestHeaders().getFirst("Authorization"));
          received.put("request", mapper.readTree(exchange.getRequestBody()));
          String content =
              mapper.writeValueAsString(
                  mapper
                      .createObjectNode()
                      .set(
                          "items",
                          mapper.valueToTree(
                              List.of(
                                  mapper
                                      .createObjectNode()
                                      .put("title", "章节一")
                                      .put("body", "这是第一段完整网页正文。")
                                      .put("summary", "摘要一")
                                      .put("category", "工程")
                                      .set("tags", mapper.valueToTree(List.of("网页", "AI"))),
                                  mapper
                                      .createObjectNode()
                                      .put("title", "章节二")
                                      .put("body", "这是第二段完整网页正文。")
                                      .put("summary", "摘要二")
                                      .put("category", "产品")
                                      .set("tags", mapper.valueToTree(List.of("整理"))),
                                  mapper
                                      .createObjectNode()
                                      .put("title", "只有网址")
                                      .put("body", "https://only.example/path")
                                      .put("summary", "摘要")
                                      .put("category", "网页")
                                      .set("tags", mapper.valueToTree(List.of("网址"))),
                                  mapper
                                      .createObjectNode()
                                      .put("title", "裸域名")
                                      .put("body", "example.com/path")
                                      .put("summary", "摘要")
                                      .put("category", "网页")
                                      .set("tags", mapper.valueToTree(List.of("网址"))),
                                  mapper
                                      .createObjectNode()
                                      .put("title", "协议相对地址")
                                      .put("body", "//example.com/path")
                                      .put("summary", "摘要")
                                      .put("category", "网页")
                                      .set("tags", mapper.valueToTree(List.of("网址"))),
                                  mapper
                                      .createObjectNode()
                                      .put("title", "FTP 地址")
                                      .put("body", "ftp://example.com/path")
                                      .put("summary", "摘要")
                                      .put("category", "网页")
                                      .set("tags", mapper.valueToTree(List.of("网址"))),
                                  mapper
                                      .createObjectNode()
                                      .put("title", "Markdown 链接")
                                      .put("body", "[阅读原文](https://example.com/path)")
                                      .put("summary", "摘要")
                                      .put("category", "网页")
                                      .set("tags", mapper.valueToTree(List.of("网址"))),
                                  mapper
                                      .createObjectNode()
                                      .put("title", "链接提示")
                                      .put("body", "链接：example.com/path")
                                      .put("summary", "摘要")
                                      .put("category", "网页")
                                      .set("tags", mapper.valueToTree(List.of("网址")))))));
          var body =
              mapper
                  .createObjectNode()
                  .set(
                      "choices",
                      mapper.valueToTree(
                          List.of(
                              mapper
                                  .createObjectNode()
                                  .set(
                                      "message",
                                      mapper.createObjectNode().put("content", content)))));
          byte[] bytes = mapper.writeValueAsBytes(body);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
    try {
      AppProperties properties =
          new AppProperties(
              new AppProperties.Owner("", "", "", false),
              new AppProperties.Storage(".data/uploads"),
              new AppProperties.Security("zhishiku_session", "/kb"),
              new AppProperties.Captcha(120, 1000, false),
              new AppProperties.Registration(
                  true, 600, 60, 3600, 5, 20, 5, 1000, false, "from@example.com", "识海"),
              new AppProperties.ExternalSearch("", "", "", "", 20, 8, 900),
              new AppProperties.Ai(
                  "http://127.0.0.1:" + server.getAddress().getPort() + "/chat/completions",
                  "test-key",
                  "deepseek-chat",
                  20),
              new AppProperties.WebImport("", "tesseract", 35, 30, 30_000, 20));
      DeepSeekKnowledgeOrganizer organizer =
          new DeepSeekKnowledgeOrganizer(properties, mapper, RestClient.builder());

      List<OrganizedKnowledge> result = organizer.organize("web page", "可见网页正文 ".repeat(50));

      assertThat(result).hasSize(2);
      assertThat(result.get(0).title()).isEqualTo("章节一");
      assertThat(received.get("authorization")).isEqualTo("Bearer test-key");
      var request = (com.fasterxml.jackson.databind.JsonNode) received.get("request");
      assertThat(request.path("model").asText()).isEqualTo("deepseek-chat");
      assertThat(request.path("response_format").path("type").asText()).isEqualTo("json_object");
      assertThat(request.path("messages").path(1).path("content").asText())
          .contains("可见网页正文")
          .doesNotContain("https://example.com/article");
    } finally {
      server.stop(0);
    }
  }
}

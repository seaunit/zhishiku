package site.seaunit.zhishiku.search.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import site.seaunit.zhishiku.config.AppProperties;

class DeepSeekWebProviderTest {
  @Test
  void mapsSearchResultsAndSendsExplicitMinimalRequest() throws IOException {
    AtomicReference<String> requestBody = new AtomicReference<>();
    AtomicReference<String> authorization = new AtomicReference<>();
    HttpServer server =
        server(
            "/v1/tools/search",
            """
            {
              "results": [
                {
                  "rerank_score": 2.7,
                  "title": "DeepSeek",
                  "url": "https://www.deepseek.com/",
                  "snippet": "DeepSeek official site",
                  "summary": "DeepSeek provides AI services.",
                  "site_name": "DeepSeek",
                  "site_icon": "https://www.deepseek.com/favicon.ico",
                  "date": "2026-10-22T00:00:00Z",
                  "authority": 4
                }
              ]
            }
            """,
            requestBody,
            authorization);
    try {
      DeepSeekWebProvider provider = provider(server);

      var results = provider.search("DeepSeek", 5);

      assertThat(results).hasSize(1);
      assertThat(results.get(0).title()).isEqualTo("DeepSeek");
      assertThat(results.get(0).siteName()).isEqualTo("DeepSeek");
      assertThat(results.get(0).authority()).isEqualTo(4);
      assertThat(authorization.get()).isEqualTo("Bearer search-key");
      assertThat(requestBody.get()).contains("\"query\":\"DeepSeek\"");
      assertThat(requestBody.get()).contains("\"need_content\":false");
    } finally {
      server.stop(0);
    }
  }

  @Test
  void fetchesOneUrlAsMarkdown() throws IOException {
    AtomicReference<String> requestBody = new AtomicReference<>();
    AtomicReference<String> authorization = new AtomicReference<>();
    HttpServer server =
        server(
            "/v1/tools/fetch",
            """
            {
              "title": "DeepSeek",
              "content": "# DeepSeek\\n\\nOfficial page content."
            }
            """,
            requestBody,
            authorization);
    try {
      DeepSeekWebProvider provider = provider(server);

      var page = provider.fetch("https://www.deepseek.com/");

      assertThat(page.title()).isEqualTo("DeepSeek");
      assertThat(page.content()).contains("Official page content");
      assertThat(authorization.get()).isEqualTo("Bearer fetch-key");
      assertThat(requestBody.get()).contains("\"url\":\"https://www.deepseek.com/\"");
    } finally {
      server.stop(0);
    }
  }

  private static DeepSeekWebProvider provider(HttpServer server) {
    String base = "http://127.0.0.1:" + server.getAddress().getPort();
    return new DeepSeekWebProvider(
        properties(base + "/v1/tools/search", base + "/v1/tools/fetch"), RestClient.builder());
  }

  private static HttpServer server(
      String path,
      String responseBody,
      AtomicReference<String> requestBody,
      AtomicReference<String> authorization)
      throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        path,
        exchange -> {
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
          byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    server.start();
    return server;
  }

  private static AppProperties properties(String searchEndpoint, String fetchEndpoint) {
    return new AppProperties(
        new AppProperties.Owner("", "", "", false),
        new AppProperties.Storage(".data/uploads"),
        new AppProperties.Security("zhishiku_session", "/kb"),
        new AppProperties.Captcha(120, 1000, false),
        new AppProperties.Registration(
            true, 600, 60, 3600, 5, 20, 5, 1000, false, "no-reply@example.com", "识海"),
        new AppProperties.ExternalSearch(
            searchEndpoint, fetchEndpoint, "search-key", "fetch-key", 20, 5, 900));
  }
}

package site.seaunit.zhishiku.search.external;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import site.seaunit.zhishiku.config.AppProperties;

@Component
public class DeepSeekWebProvider {
  private final AppProperties.ExternalSearch config;
  private final RestClient client;

  public DeepSeekWebProvider(AppProperties properties, RestClient.Builder builder) {
    this.config = properties.externalSearch();
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    int timeout = Math.max(5, config.timeoutSeconds());
    requestFactory.setConnectTimeout(Duration.ofSeconds(timeout));
    requestFactory.setReadTimeout(Duration.ofSeconds(timeout));
    this.client = builder.requestFactory(requestFactory).build();
  }

  public List<SearchResult> search(String query, int count) {
    requireConfigured(config.searchEndpoint(), config.searchApiKey());
    JsonNode response =
        post(
            config.searchEndpoint(),
            config.searchApiKey(),
            Map.of("query", query, "count", count, "need_content", true));
    JsonNode results = response.path("results");
    if (!results.isArray()) {
      throw new ExternalSearchUnavailableException("Search provider returned an invalid response.");
    }
    List<SearchResult> mapped = new ArrayList<>();
    for (JsonNode item : results) {
      String title = item.path("title").asText("").strip();
      String url = item.path("url").asText("").strip();
      if (title.isBlank() || !isHttpUrl(url)) {
        continue;
      }
      mapped.add(
          new SearchResult(
              title,
              url,
              item.path("snippet").asText(""),
              item.path("summary").asText(""),
              nullableText(item, "site_name"),
              nullableText(item, "site_icon"),
              nullableText(item, "date"),
              item.path("authority").asInt(-1),
              item.path("rerank_score").asDouble(0),
              item.path("content").asText("")));
    }
    return mapped;
  }

  public FetchedPage fetch(String url) {
    requireConfigured(config.fetchEndpoint(), config.fetchApiKey());
    if (!isHttpUrl(url)) {
      throw new IllegalArgumentException("Only HTTP and HTTPS URLs can be fetched.");
    }
    JsonNode response;
    try {
      response = post(config.fetchEndpoint(), config.fetchApiKey(), Map.of("url", url));
    } catch (ExternalSearchUnavailableException error) {
      if (Integer.valueOf(404).equals(error.upstreamStatus())) {
        throw new ExternalSourceUnavailableException(
            "The selected source could not be fetched. Choose another search result.");
      }
      throw error;
    }
    String title = response.path("title").asText("").strip();
    String content = response.path("content").asText("").strip();
    if (content.isBlank()) {
      throw new ExternalSourceUnavailableException(
          "The selected source did not contain usable content. Choose another search result.");
    }
    return new FetchedPage(title, content);
  }

  private JsonNode post(String endpoint, String apiKey, Map<String, ?> payload) {
    try {
      JsonNode body =
          client
              .post()
              .uri(endpoint)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .body(payload)
              .retrieve()
              .body(JsonNode.class);
      if (body == null) {
        throw new ExternalSearchUnavailableException("Search provider returned an empty response.");
      }
      return body;
    } catch (RestClientResponseException error) {
      throw new ExternalSearchUnavailableException(
          "Search provider rejected the request with HTTP " + error.getStatusCode().value() + ".",
          error.getStatusCode().value());
    } catch (ExternalSearchUnavailableException error) {
      throw error;
    } catch (RuntimeException error) {
      throw new ExternalSearchUnavailableException("Search provider is temporarily unavailable.");
    }
  }

  private static void requireConfigured(String endpoint, String apiKey) {
    if (endpoint == null || endpoint.isBlank() || apiKey == null || apiKey.isBlank()) {
      throw new ExternalSearchUnavailableException("Internet search is not configured.");
    }
  }

  private static boolean isHttpUrl(String value) {
    try {
      var uri = java.net.URI.create(value);
      return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
          && uri.getHost() != null
          && uri.getUserInfo() == null;
    } catch (IllegalArgumentException error) {
      return false;
    }
  }

  private static String nullableText(JsonNode node, String field) {
    JsonNode value = node.get(field);
    return value == null || value.isNull() ? null : value.asText();
  }

  public record SearchResult(
      String title,
      String url,
      String snippet,
      String summary,
      String siteName,
      String siteIcon,
      String date,
      int authority,
      double rerankScore,
      String content) {}

  public record FetchedPage(String title, String content) {}
}

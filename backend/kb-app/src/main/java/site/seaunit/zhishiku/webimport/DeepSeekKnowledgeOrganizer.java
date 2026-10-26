package site.seaunit.zhishiku.webimport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import site.seaunit.zhishiku.config.AppProperties;

@Component
@ConditionalOnProperty(
    prefix = "app.ai-orchestration",
    name = "provider",
    havingValue = "deepseek",
    matchIfMissing = true)
public class DeepSeekKnowledgeOrganizer
    implements WebPageOrganizer,
        DocumentOrganizer,
        ScreenshotOrganizer,
        TextOrganizer,
        InternetResultOrganizer {
  private static final String SYSTEM_PROMPT =
      """
      You convert one {{SOURCE_KIND}} into self-contained knowledge notes.
      Return JSON only: {"items":[{"title":"...","body":"...","summary":"...","category":"...","tags":["..."]}]}.
      Create between 1 and %d notes. Split only when the source contains distinct topics.
      Preserve factual meaning and the source language. Do not invent details.
      Body must be a polished Markdown subset: ##/### headings, short paragraphs, bullet or numbered
      lists, > blockquotes and fenced code where useful. Do not use HTML, scripts, tables, remote
      image syntax, Markdown image URLs, or URLs-only filler. Write for readable study notes, not a
      chat summary.
      """;

  private final AppProperties.Ai config;
  private final int maxItems;
  private final ObjectMapper mapper;
  private final RestClient client;

  public DeepSeekKnowledgeOrganizer(
      AppProperties properties, ObjectMapper mapper, RestClient.Builder builder) {
    this.config = properties.ai();
    this.maxItems = Math.max(1, properties.webImport().maxItems());
    this.mapper = mapper;
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    int timeout = Math.max(10, config.timeoutSeconds());
    requestFactory.setConnectTimeout(Duration.ofSeconds(timeout));
    requestFactory.setReadTimeout(Duration.ofSeconds(timeout));
    this.client = builder.requestFactory(requestFactory).build();
  }

  @Override
  public List<OrganizedKnowledge> organize(String sourceLabel, String text) {
    if (config.apiKey() == null || config.apiKey().isBlank()) {
      throw new WebImportUnavailableException("AI page organization is not configured.");
    }
    Map<String, Object> request = new LinkedHashMap<>();
    request.put("model", config.model());
    request.put(
        "messages",
        List.of(
            Map.of(
                "role",
                "system",
                "content",
                SYSTEM_PROMPT.replace("{{SOURCE_KIND}}", sourceLabel).formatted(maxItems)),
            Map.of("role", "user", "content", "Extracted source text:\n" + text)));
    request.put("response_format", Map.of("type", "json_object"));
    request.put("temperature", 0.2);
    request.put("max_tokens", 8000);

    try {
      JsonNode response =
          client
              .post()
              .uri(config.endpoint())
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.apiKey())
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .body(request)
              .retrieve()
              .body(JsonNode.class);
      if (response == null) {
        throw new WebImportUnavailableException("AI page organization returned no response.");
      }
      String content = response.path("choices").path(0).path("message").path("content").asText("");
      if (content.isBlank()) {
        throw new WebPageOrganizationException("AI page organization returned no knowledge.");
      }
      return OrganizedKnowledgeParser.parse(mapper, maxItems, content);
    } catch (WebImportUnavailableException error) {
      throw error;
    } catch (WebPageOrganizationException error) {
      throw error;
    } catch (RuntimeException error) {
      throw new WebImportUnavailableException("AI page organization is temporarily unavailable.");
    }
  }
}

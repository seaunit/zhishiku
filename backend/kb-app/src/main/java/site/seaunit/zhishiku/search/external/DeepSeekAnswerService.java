package site.seaunit.zhishiku.search.external;

import com.fasterxml.jackson.databind.JsonNode;
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
public class DeepSeekAnswerService implements ExternalAnswerService {
  private final AppProperties.Ai config;
  private final RestClient client;

  public DeepSeekAnswerService(AppProperties properties, RestClient.Builder builder) {
    this.config = properties.ai();
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    int timeout = Math.max(10, config.timeoutSeconds());
    requestFactory.setConnectTimeout(Duration.ofSeconds(timeout));
    requestFactory.setReadTimeout(Duration.ofSeconds(timeout));
    this.client = builder.requestFactory(requestFactory).build();
  }

  @Override
  public String answer(String query, List<DeepSeekWebProvider.SearchResult> results) {
    if (results.isEmpty() || config.apiKey() == null || config.apiKey().isBlank()) {
      return null;
    }
    String evidence =
        results.stream()
            .limit(5)
            .map(
                result ->
                    result.title()
                        + "\n"
                        + result.url()
                        + "\n"
                        + (result.summary().isBlank() ? result.snippet() : result.summary()))
            .collect(java.util.stream.Collectors.joining("\n\n"));
    Map<String, Object> request = new LinkedHashMap<>();
    request.put("model", config.model());
    request.put(
        "messages",
        List.of(
            Map.of(
                "role",
                "system",
                "content",
                "Answer the user's search question only from the supplied evidence. "
                    + "If evidence is insufficient, say so explicitly. Return plain text only."),
            Map.of(
                "role", "user", "content", "Question:\n" + query + "\n\nEvidence:\n" + evidence)));
    request.put("temperature", 0.1);
    request.put("max_tokens", 900);
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
    String answer =
        response == null
            ? ""
            : response.path("choices").path(0).path("message").path("content").asText("").strip();
    return answer.isBlank() ? null : answer;
  }
}

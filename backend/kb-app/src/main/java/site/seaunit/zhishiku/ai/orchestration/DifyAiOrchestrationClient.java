package site.seaunit.zhishiku.ai.orchestration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.ai-orchestration", name = "provider", havingValue = "dify")
public class DifyAiOrchestrationClient implements AiOrchestrationClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(DifyAiOrchestrationClient.class);
  private static final String EVENT_TEXT_CHUNK = "text_chunk";
  private static final String EVENT_WORKFLOW_FINISHED = "workflow_finished";
  private static final String EVENT_ERROR = "error";
  private static final String REASONING_OPEN = "<think>";
  private static final String REASONING_CLOSE = "</think>";

  private final AiOrchestrationProperties properties;
  private final ObjectMapper mapper;
  private final Clock clock;
  private final HttpClient httpClient;

  public DifyAiOrchestrationClient(
      AiOrchestrationProperties properties, ObjectMapper mapper, Clock clock) {
    this.properties = properties;
    this.mapper = mapper;
    this.clock = clock;
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(properties.timeoutSeconds()))
            .build();
  }

  @Override
  public String run(
      AiOrchestrationWorkflow workflow, Map<String, String> inputs, String externalUser) {
    String key = apiKey(workflow);
    long startedAt = clock.millis();
    AiOrchestrationException lastFailure = null;
    for (int attempt = 0; attempt <= properties.maxRetries(); attempt++) {
      try {
        HttpRequest request =
            request(workflow, inputs, externalUser, key, "blocking")
                .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .build();
        HttpResponse<String> response =
            httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
          String result = parseBlockingResponse(response.body());
          logSuccess(workflow, startedAt, null);
          return result;
        }
        lastFailure = statusFailure(response.statusCode());
        if (!retryableStatus(response.statusCode()) || attempt >= properties.maxRetries()) {
          throw lastFailure;
        }
      } catch (AiOrchestrationException error) {
        lastFailure = error;
        if (!isRetryableCode(error.code()) || attempt >= properties.maxRetries()) {
          throw error;
        }
      } catch (InterruptedException error) {
        Thread.currentThread().interrupt();
        throw new AiOrchestrationException(
            "INTERRUPTED", "AI orchestration was interrupted.", error);
      } catch (IOException | IllegalArgumentException error) {
        lastFailure =
            new AiOrchestrationException(
                "UNAVAILABLE", "AI orchestration service is unavailable.", error);
        if (attempt >= properties.maxRetries()) {
          throw lastFailure;
        }
      }
    }
    throw lastFailure == null
        ? new AiOrchestrationException("UNAVAILABLE", "AI orchestration failed.")
        : lastFailure;
  }

  @Override
  public String stream(
      AiOrchestrationWorkflow workflow,
      Map<String, String> inputs,
      String externalUser,
      Consumer<String> tokenConsumer) {
    if (tokenConsumer == null) {
      throw new IllegalArgumentException("Token consumer is required.");
    }
    String key = apiKey(workflow);
    long startedAt = clock.millis();
    try {
      HttpRequest request =
          request(workflow, inputs, externalUser, key, "streaming")
              .header("Accept", "text/event-stream")
              .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
              .build();
      HttpResponse<Stream<String>> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        response.body().close();
        throw statusFailure(response.statusCode());
      }

      StringBuilder rawStream = new StringBuilder();
      StringBuilder visibleStream = new StringBuilder();
      ReasoningStreamGate reasoningGate =
          new ReasoningStreamGate(
              text -> {
                visibleStream.append(text);
                tokenConsumer.accept(text);
              });
      String fallback = "";
      boolean finished = false;
      try (Stream<String> lines = response.body()) {
        for (String line : (Iterable<String>) lines::iterator) {
          if (!line.startsWith("data:")) {
            continue;
          }
          String payload = line.substring(5).strip();
          if (payload.isBlank() || "[DONE]".equals(payload)) {
            continue;
          }
          JsonNode event = mapper.readTree(payload);
          String eventName = event.path("event").asText("");
          if (EVENT_TEXT_CHUNK.equals(eventName)) {
            String text = event.path("data").path("text").asText("");
            if (!text.isEmpty()) {
              rawStream.append(text);
              reasoningGate.accept(text);
            }
          } else if (EVENT_WORKFLOW_FINISHED.equals(eventName)) {
            String status = event.path("data").path("status").asText("");
            if (!"succeeded".equals(status)) {
              throw new AiOrchestrationException(
                  "WORKFLOW_FAILED", "AI orchestration workflow did not succeed.");
            }
            fallback = event.path("data").path("outputs").path("result").asText("");
            finished = true;
          } else if (EVENT_ERROR.equals(eventName)) {
            throw new AiOrchestrationException(
                "WORKFLOW_ERROR", "AI orchestration workflow reported an error.");
          }
        }
      }
      if (!finished) {
        throw new AiOrchestrationException(
            "INCOMPLETE_STREAM", "AI orchestration stream ended before completion.");
      }
      if (rawStream.isEmpty() && fallback != null && !fallback.isBlank()) {
        reasoningGate.accept(fallback);
      }
      reasoningGate.finish();
      String result = visibleStream.toString().strip();
      if (result.isBlank()) {
        throw new AiOrchestrationException(
            "EMPTY_RESULT", "AI orchestration returned no usable result.");
      }
      logSuccess(workflow, startedAt, null);
      return result;
    } catch (AiOrchestrationException error) {
      logFailure(workflow, startedAt, error.code());
      throw error;
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      logFailure(workflow, startedAt, "INTERRUPTED");
      throw new AiOrchestrationException(
          "INTERRUPTED", "AI orchestration stream was interrupted.", error);
    } catch (JsonProcessingException error) {
      logFailure(workflow, startedAt, "MALFORMED_STREAM");
      throw new AiOrchestrationException(
          "MALFORMED_STREAM", "AI orchestration stream returned malformed data.", error);
    } catch (IOException | IllegalArgumentException error) {
      logFailure(workflow, startedAt, "UNAVAILABLE");
      throw new AiOrchestrationException(
          "UNAVAILABLE", "AI orchestration stream is unavailable.", error);
    }
  }

  private HttpRequest.Builder request(
      AiOrchestrationWorkflow workflow,
      Map<String, String> inputs,
      String externalUser,
      String key,
      String responseMode) {
    if (externalUser == null || externalUser.isBlank() || externalUser.length() > 100) {
      throw new IllegalArgumentException("External user identifier is invalid.");
    }
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("inputs", inputs == null ? Map.of() : inputs);
    body.put("response_mode", responseMode);
    body.put("user", externalUser);
    try {
      return HttpRequest.newBuilder(URI.create(properties.difyBaseUrl() + "/workflows/run"))
          .header("Authorization", "Bearer " + key)
          .header("Content-Type", "application/json")
          .POST(
              HttpRequest.BodyPublishers.ofString(
                  mapper.writeValueAsString(body), StandardCharsets.UTF_8));
    } catch (JsonProcessingException error) {
      throw new AiOrchestrationException(
          "REQUEST_INVALID", "AI orchestration request could not be encoded.", error);
    }
  }

  private String apiKey(AiOrchestrationWorkflow workflow) {
    String key =
        workflow == AiOrchestrationWorkflow.ORGANIZER
            ? properties.difyOrganizerApiKey()
            : properties.difyChatApiKey();
    if (key == null || key.isBlank()) {
      throw new AiOrchestrationException("NOT_CONFIGURED", "AI orchestration is not configured.");
    }
    return key.strip();
  }

  private String parseBlockingResponse(String body) {
    try {
      JsonNode root = mapper.readTree(body);
      String status = root.path("data").path("status").asText("");
      if (!"succeeded".equals(status)) {
        throw new AiOrchestrationException(
            "WORKFLOW_FAILED", "AI orchestration workflow did not succeed.");
      }
      String result = root.path("data").path("outputs").path("result").asText("");
      if (result.isBlank()) {
        throw new AiOrchestrationException(
            "EMPTY_RESULT", "AI orchestration returned no usable result.");
      }
      return result;
    } catch (JsonProcessingException error) {
      throw new AiOrchestrationException(
          "MALFORMED_RESPONSE", "AI orchestration returned malformed data.", error);
    }
  }

  private static AiOrchestrationException statusFailure(int status) {
    if (status == 401 || status == 403) {
      return new AiOrchestrationException("AUTH_FAILED", "AI orchestration authentication failed.");
    }
    if (status == 429) {
      return new AiOrchestrationException(
          "RATE_LIMITED", "AI orchestration rate limit was exceeded.");
    }
    if (status >= 500) {
      return new AiOrchestrationException(
          "UNAVAILABLE", "AI orchestration service is unavailable.");
    }
    return new AiOrchestrationException("REJECTED", "AI orchestration request was rejected.");
  }

  private static boolean retryableStatus(int status) {
    return status >= 500;
  }

  private static boolean isRetryableCode(String code) {
    return "UNAVAILABLE".equals(code) || "RATE_LIMITED".equals(code);
  }

  private void logSuccess(AiOrchestrationWorkflow workflow, long startedAt, String runId) {
    LOGGER.info(
        "ai_orchestration provider=dify workflow={} status=succeeded elapsed_ms={} run_id={}",
        workflow.name().toLowerCase(),
        clock.millis() - startedAt,
        runId == null ? "-" : runId);
  }

  private void logFailure(AiOrchestrationWorkflow workflow, long startedAt, String code) {
    LOGGER.warn(
        "ai_orchestration provider=dify workflow={} status=failed code={} elapsed_ms={}",
        workflow.name().toLowerCase(),
        code,
        clock.millis() - startedAt);
  }

  private static final class ReasoningStreamGate {
    private final Consumer<String> target;
    private final StringBuilder pending = new StringBuilder();
    private boolean resolved;
    private boolean inReasoning;

    private ReasoningStreamGate(Consumer<String> target) {
      this.target = target;
    }

    private void accept(String text) {
      if (text == null || text.isEmpty()) {
        return;
      }
      if (resolved) {
        target.accept(text);
        return;
      }
      pending.append(text);
      while (pending.length() > 0) {
        if (inReasoning) {
          int close = pending.indexOf(REASONING_CLOSE);
          if (close < 0) {
            return;
          }
          pending.delete(0, close + REASONING_CLOSE.length());
          inReasoning = false;
          continue;
        }

        int start = 0;
        while (start < pending.length() && Character.isWhitespace(pending.charAt(start))) {
          start++;
        }
        if (start == pending.length()) {
          pending.setLength(0);
          return;
        }
        String value = pending.substring(start);
        if (REASONING_OPEN.startsWith(value)) {
          return;
        }
        if (value.startsWith(REASONING_OPEN)) {
          pending.delete(0, start + REASONING_OPEN.length());
          inReasoning = true;
          continue;
        }
        resolved = true;
        target.accept(value);
        pending.setLength(0);
        return;
      }
    }

    private void finish() {
      pending.setLength(0);
      resolved = true;
    }
  }
}

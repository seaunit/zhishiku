package site.seaunit.zhishiku.ai.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DifyAiOrchestrationClientTest {
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void sendsScopedBlockingRequestAndReturnsWorkflowResult() throws Exception {
    List<JsonNode> requests = new ArrayList<>();
    List<String> authorizations = new ArrayList<>();
    HttpServer server =
        server(
            exchange -> {
              requests.add(mapper.readTree(exchange.getRequestBody()));
              authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
              respondJson(exchange, blockingSuccess("知识结果"));
            });
    try (TestServer running = new TestServer(server)) {
      DifyAiOrchestrationClient client = client(running.baseUrl());

      String result =
          client.run(
              AiOrchestrationWorkflow.ORGANIZER,
              Map.of("operation", "knowledge_organize", "content", "脱敏文本"),
              "account-1");

      assertThat(result).isEqualTo("知识结果");
      assertThat(authorizations).containsExactly("Bearer organizer-key");
      JsonNode request = requests.get(0);
      assertThat(request.path("response_mode").asText()).isEqualTo("blocking");
      assertThat(request.path("user").asText()).isEqualTo("account-1");
      assertThat(request.path("inputs").path("content").asText()).isEqualTo("脱敏文本");
    }
  }

  @Test
  void convertsTextChunksWithoutExposingRawEvents() throws Exception {
    HttpServer server =
        server(
            exchange -> {
              exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
              exchange.sendResponseHeaders(200, 0);
              String events =
                  "data: {\"event\":\"text_chunk\",\"data\":{\"text\":\"你\"}}\n\n"
                      + "data: {\"event\":\"text_chunk\",\"data\":{\"text\":\"好\"}}\n\n"
                      + "data: {\"event\":\"workflow_finished\",\"data\":{\"status\":\"succeeded\",\"outputs\":{\"result\":\"你好\"}}}\n\n";
              exchange.getResponseBody().write(events.getBytes(StandardCharsets.UTF_8));
              exchange.close();
            });
    try (TestServer running = new TestServer(server)) {
      DifyAiOrchestrationClient client = client(running.baseUrl());
      List<String> tokens = new ArrayList<>();

      String result =
          client.stream(
              AiOrchestrationWorkflow.CHAT,
              Map.of("operation", "stream_general", "question", "你好"),
              "account-1",
              tokens::add);

      assertThat(result).isEqualTo("你好");
      assertThat(tokens).containsExactly("你", "好");
    }
  }

  @Test
  void hidesLeadingReasoningBlockFromStreamingUsers() throws Exception {
    HttpServer server =
        server(
            exchange -> {
              exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
              exchange.sendResponseHeaders(200, 0);
              String events =
                  "data: {\"event\":\"text_chunk\",\"data\":{\"text\":\"<thi\"}}\n\n"
                      + "data: {\"event\":\"text_chunk\",\"data\":{\"text\":\"nk>内部推理</think>\"}}\n\n"
                      + "data: {\"event\":\"text_chunk\",\"data\":{\"text\":\"正式回答\"}}\n\n"
                      + "data: {\"event\":\"workflow_finished\",\"data\":{\"status\":\"succeeded\",\"outputs\":{\"result\":\"正式回答\"}}}\n\n";
              exchange.getResponseBody().write(events.getBytes(StandardCharsets.UTF_8));
              exchange.close();
            });
    try (TestServer running = new TestServer(server)) {
      DifyAiOrchestrationClient client = client(running.baseUrl());
      List<String> tokens = new ArrayList<>();

      String result =
          client.stream(
              AiOrchestrationWorkflow.CHAT,
              Map.of("operation", "stream_general", "question", "你好"),
              "account-1",
              tokens::add);

      assertThat(result).isEqualTo("正式回答");
      assertThat(tokens).containsExactly("正式回答");
    }
  }

  @Test
  void rejectsFailedWorkflowAndMissingApiKey() throws Exception {
    HttpServer server =
        server(
            exchange -> respondJson(exchange, "{\"data\":{\"status\":\"failed\",\"outputs\":{}}}"));
    try (TestServer running = new TestServer(server)) {
      DifyAiOrchestrationClient client = client(running.baseUrl());

      assertThatThrownBy(
              () ->
                  client.run(
                      AiOrchestrationWorkflow.CHAT,
                      Map.of("operation", "general_answer"),
                      "account-1"))
          .isInstanceOf(AiOrchestrationException.class)
          .extracting(error -> ((AiOrchestrationException) error).code())
          .isEqualTo("WORKFLOW_FAILED");
    }

    AiOrchestrationProperties missingKeys =
        new AiOrchestrationProperties("dify", "http://127.0.0.1:1/v1", "", "", 10, 0);
    DifyAiOrchestrationClient missingClient =
        new DifyAiOrchestrationClient(missingKeys, mapper, Clock.systemUTC());
    assertThatThrownBy(
            () ->
                missingClient.run(
                    AiOrchestrationWorkflow.ORGANIZER,
                    Map.of("operation", "knowledge_organize"),
                    "account-1"))
        .isInstanceOf(AiOrchestrationException.class)
        .extracting(error -> ((AiOrchestrationException) error).code())
        .isEqualTo("NOT_CONFIGURED");
  }

  @Test
  void rejectsAuthenticationFailureAndMalformedResponse() throws Exception {
    HttpServer unauthorized =
        server(
            exchange -> {
              exchange.sendResponseHeaders(401, -1);
              exchange.close();
            });
    try (TestServer running = new TestServer(unauthorized)) {
      DifyAiOrchestrationClient client = client(running.baseUrl());
      assertThatThrownBy(
              () ->
                  client.run(
                      AiOrchestrationWorkflow.CHAT,
                      Map.of("operation", "general_answer"),
                      "account-1"))
          .isInstanceOf(AiOrchestrationException.class)
          .extracting(error -> ((AiOrchestrationException) error).code())
          .isEqualTo("AUTH_FAILED");
    }

    HttpServer malformed = server(exchange -> respondJson(exchange, "not-json"));
    try (TestServer running = new TestServer(malformed)) {
      DifyAiOrchestrationClient client = client(running.baseUrl());
      assertThatThrownBy(
              () ->
                  client.run(
                      AiOrchestrationWorkflow.ORGANIZER,
                      Map.of("operation", "knowledge_organize"),
                      "account-1"))
          .isInstanceOf(AiOrchestrationException.class)
          .extracting(error -> ((AiOrchestrationException) error).code())
          .isEqualTo("MALFORMED_RESPONSE");
    }
  }

  @Test
  void rejectsErrorAndIncompleteStreamEvents() throws Exception {
    HttpServer errorStream =
        server(
            exchange -> {
              exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
              exchange.sendResponseHeaders(200, 0);
              exchange
                  .getResponseBody()
                  .write(
                      "data: {\"event\":\"error\",\"data\":{\"message\":\"hidden\"}}\n\n"
                          .getBytes(StandardCharsets.UTF_8));
              exchange.close();
            });
    try (TestServer running = new TestServer(errorStream)) {
      DifyAiOrchestrationClient client = client(running.baseUrl());
      assertThatThrownBy(
              () ->
                  client.stream(
                      AiOrchestrationWorkflow.CHAT,
                      Map.of("operation", "stream_general"),
                      "account-1",
                      ignored -> {}))
          .isInstanceOf(AiOrchestrationException.class)
          .extracting(error -> ((AiOrchestrationException) error).code())
          .isEqualTo("WORKFLOW_ERROR");
    }

    HttpServer incomplete =
        server(
            exchange -> {
              exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
              exchange.sendResponseHeaders(200, 0);
              exchange
                  .getResponseBody()
                  .write(
                      "data: {\"event\":\"text_chunk\",\"data\":{\"text\":\"半段\"}}\n\n"
                          .getBytes(StandardCharsets.UTF_8));
              exchange.close();
            });
    try (TestServer running = new TestServer(incomplete)) {
      DifyAiOrchestrationClient client = client(running.baseUrl());
      assertThatThrownBy(
              () ->
                  client.stream(
                      AiOrchestrationWorkflow.CHAT,
                      Map.of("operation", "stream_general"),
                      "account-1",
                      ignored -> {}))
          .isInstanceOf(AiOrchestrationException.class)
          .extracting(error -> ((AiOrchestrationException) error).code())
          .isEqualTo("INCOMPLETE_STREAM");
    }
  }

  private DifyAiOrchestrationClient client(String baseUrl) {
    return new DifyAiOrchestrationClient(
        new AiOrchestrationProperties("dify", baseUrl, "organizer-key", "chat-key", 10, 0),
        mapper,
        Clock.systemUTC());
  }

  private HttpServer server(ExchangeHandler handler) throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/v1/workflows/run",
        exchange -> {
          try {
            handler.handle(exchange);
          } catch (Exception error) {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
          }
        });
    server.start();
    return server;
  }

  private void respondJson(HttpExchange exchange, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }

  private static String blockingSuccess(String result) {
    return "{\"workflow_run_id\":\"run-1\",\"data\":{\"status\":\"succeeded\","
        + "\"outputs\":{\"result\":\""
        + result
        + "\"}}}";
  }

  @FunctionalInterface
  private interface ExchangeHandler {
    void handle(HttpExchange exchange) throws Exception;
  }

  private final class TestServer implements AutoCloseable {
    private final HttpServer server;

    private TestServer(HttpServer server) {
      this.server = server;
    }

    private String baseUrl() {
      return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
    }

    @Override
    public void close() {
      server.stop(0);
    }
  }
}

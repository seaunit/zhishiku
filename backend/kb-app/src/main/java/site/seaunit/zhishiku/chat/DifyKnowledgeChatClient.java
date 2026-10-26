package site.seaunit.zhishiku.chat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import site.seaunit.zhishiku.ai.orchestration.AiExternalUserResolver;
import site.seaunit.zhishiku.ai.orchestration.AiOrchestrationClient;
import site.seaunit.zhishiku.ai.orchestration.AiOrchestrationException;
import site.seaunit.zhishiku.ai.orchestration.AiOrchestrationWorkflow;
import site.seaunit.zhishiku.webimport.WebImportUnavailableException;

@Component
@ConditionalOnProperty(prefix = "app.ai-orchestration", name = "provider", havingValue = "dify")
public class DifyKnowledgeChatClient implements KnowledgeChatModelClient {
  private static final String INSUFFICIENT_EVIDENCE = "INSUFFICIENT_EVIDENCE";

  private final AiOrchestrationClient client;
  private final Supplier<String> externalUser;

  @Autowired
  public DifyKnowledgeChatClient(
      AiOrchestrationClient client, AiExternalUserResolver externalUsers) {
    this(client, externalUsers::resolve);
  }

  DifyKnowledgeChatClient(AiOrchestrationClient client, Supplier<String> externalUser) {
    this.client = client;
    this.externalUser = externalUser;
  }

  @Override
  public ModelAnswer answer(
      String question, List<HistoryMessage> history, List<Evidence> evidence) {
    if (evidence.isEmpty()) {
      return new ModelAnswer("", List.of(), true);
    }
    String result = run("knowledge_answer", question, history, evidenceWithIds(evidence));
    return DeepSeekKnowledgeChatClient.parse(result);
  }

  @Override
  public ModelAnswer answerGenerally(String question, List<HistoryMessage> history) {
    String result = run("general_answer", question, history, "");
    return DeepSeekKnowledgeChatClient.parse(result);
  }

  @Override
  public List<String> planQueries(String question) {
    String result = run("query_plan", question, List.of(), "");
    return DeepSeekKnowledgeChatClient.parseQueries(result);
  }

  @Override
  public String streamAnswer(
      String question,
      List<HistoryMessage> history,
      List<Evidence> evidence,
      Consumer<String> tokenConsumer) {
    if (evidence.isEmpty()) {
      return INSUFFICIENT_EVIDENCE;
    }
    return stream("stream_knowledge", question, history, numberedEvidence(evidence), tokenConsumer);
  }

  @Override
  public String streamGeneralAnswer(
      String question, List<HistoryMessage> history, Consumer<String> tokenConsumer) {
    return stream("stream_general", question, history, "", tokenConsumer);
  }

  private String run(
      String operation, String question, List<HistoryMessage> history, String evidence) {
    try {
      return client.run(
          AiOrchestrationWorkflow.CHAT,
          inputs(operation, question, history, evidence),
          externalUser.get());
    } catch (AiOrchestrationException error) {
      throw new WebImportUnavailableException("Knowledge chat AI is temporarily unavailable.");
    }
  }

  private String stream(
      String operation,
      String question,
      List<HistoryMessage> history,
      String evidence,
      Consumer<String> tokenConsumer) {
    try {
      return client.stream(
          AiOrchestrationWorkflow.CHAT,
          inputs(operation, question, history, evidence),
          externalUser.get(),
          tokenConsumer);
    } catch (AiOrchestrationException error) {
      throw new WebImportUnavailableException(
          "Knowledge chat streaming is temporarily unavailable.");
    }
  }

  private static Map<String, String> inputs(
      String operation, String question, List<HistoryMessage> history, String evidence) {
    Map<String, String> inputs = new LinkedHashMap<>();
    inputs.put("operation", operation);
    inputs.put("question", safe(question, 4_000));
    inputs.put("history", historyText(history));
    inputs.put("evidence", safe(evidence, 30_000));
    return Map.copyOf(inputs);
  }

  private static String historyText(List<HistoryMessage> history) {
    if (history == null || history.isEmpty()) {
      return "";
    }
    StringBuilder text = new StringBuilder();
    for (HistoryMessage message : history) {
      if (message == null || message.content() == null || message.content().isBlank()) {
        continue;
      }
      if (!text.isEmpty()) {
        text.append('\n');
      }
      text.append(message.role()).append(": ").append(message.content().strip());
    }
    return safe(text.toString(), 8_000);
  }

  private static String evidenceWithIds(List<Evidence> evidence) {
    StringBuilder text = new StringBuilder();
    for (Evidence item : evidence) {
      text.append('[').append(safe(item.id(), 500)).append(']');
      text.append("\nTitle: ").append(safe(item.title(), 300)).append('\n');
      if (item.url() != null && !item.url().isBlank()) {
        text.append("Source: ").append(safe(item.url(), 1_000)).append('\n');
      }
      text.append(safe(item.content(), 3_500)).append("\n\n");
    }
    return safe(text.toString(), 30_000);
  }

  private static String numberedEvidence(List<Evidence> evidence) {
    StringBuilder text = new StringBuilder();
    for (int index = 0; index < evidence.size(); index++) {
      Evidence item = evidence.get(index);
      text.append('[').append(index + 1).append("] Title: ");
      text.append(safe(item.title(), 300)).append('\n');
      if (item.url() != null && !item.url().isBlank()) {
        text.append("Source: ").append(safe(item.url(), 1_000)).append('\n');
      }
      text.append(safe(item.content(), 3_500)).append("\n\n");
    }
    return safe(text.toString(), 30_000);
  }

  private static String safe(String value, int maxLength) {
    String normalized = value == null ? "" : value.strip();
    return normalized.substring(0, Math.min(normalized.length(), maxLength));
  }
}

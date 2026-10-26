package site.seaunit.zhishiku.search.external;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import site.seaunit.zhishiku.ai.orchestration.AiExternalUserResolver;
import site.seaunit.zhishiku.ai.orchestration.AiOrchestrationClient;
import site.seaunit.zhishiku.ai.orchestration.AiOrchestrationWorkflow;
import site.seaunit.zhishiku.chat.DeepSeekKnowledgeChatClient;
import site.seaunit.zhishiku.chat.KnowledgeChatModelClient.ModelAnswer;

@Component
@ConditionalOnProperty(prefix = "app.ai-orchestration", name = "provider", havingValue = "dify")
public class DifyAnswerService implements ExternalAnswerService {
  private final AiOrchestrationClient client;
  private final AiExternalUserResolver externalUsers;

  public DifyAnswerService(AiOrchestrationClient client, AiExternalUserResolver externalUsers) {
    this.client = client;
    this.externalUsers = externalUsers;
  }

  @Override
  public String answer(String query, List<DeepSeekWebProvider.SearchResult> results) {
    if (results == null || results.isEmpty()) {
      return null;
    }
    StringBuilder evidence = new StringBuilder();
    for (int index = 0; index < results.size() && index < 5; index++) {
      DeepSeekWebProvider.SearchResult result = results.get(index);
      evidence.append('[').append(index + 1).append("] ").append(result.title()).append('\n');
      evidence.append(result.url()).append('\n');
      evidence
          .append(result.summary().isBlank() ? result.snippet() : result.summary())
          .append("\n\n");
    }
    Map<String, String> inputs = new LinkedHashMap<>();
    inputs.put("operation", "internet_answer");
    inputs.put("question", query == null ? "" : query);
    inputs.put("history", "");
    inputs.put("evidence", evidence.toString());
    try {
      ModelAnswer answer =
          DeepSeekKnowledgeChatClient.parse(
              client.run(
                  AiOrchestrationWorkflow.CHAT, Map.copyOf(inputs), externalUsers.resolve()));
      return answer.answer().isBlank() ? null : answer.answer();
    } catch (RuntimeException error) {
      return null;
    }
  }
}

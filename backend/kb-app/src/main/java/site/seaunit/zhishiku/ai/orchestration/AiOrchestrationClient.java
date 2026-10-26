package site.seaunit.zhishiku.ai.orchestration;

import java.util.Map;
import java.util.function.Consumer;

public interface AiOrchestrationClient {
  String run(AiOrchestrationWorkflow workflow, Map<String, String> inputs, String externalUser);

  String stream(
      AiOrchestrationWorkflow workflow,
      Map<String, String> inputs,
      String externalUser,
      Consumer<String> tokenConsumer);
}

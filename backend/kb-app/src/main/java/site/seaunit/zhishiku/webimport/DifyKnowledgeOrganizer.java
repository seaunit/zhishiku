package site.seaunit.zhishiku.webimport;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import site.seaunit.zhishiku.ai.orchestration.AiExternalUserResolver;
import site.seaunit.zhishiku.ai.orchestration.AiOrchestrationClient;
import site.seaunit.zhishiku.ai.orchestration.AiOrchestrationException;
import site.seaunit.zhishiku.ai.orchestration.AiOrchestrationWorkflow;
import site.seaunit.zhishiku.config.AppProperties;

@Component
@ConditionalOnProperty(prefix = "app.ai-orchestration", name = "provider", havingValue = "dify")
public class DifyKnowledgeOrganizer
    implements WebPageOrganizer,
        DocumentOrganizer,
        ScreenshotOrganizer,
        TextOrganizer,
        InternetResultOrganizer {
  private final AiOrchestrationClient client;
  private final ObjectMapper mapper;
  private final int maxItems;
  private final Supplier<String> externalUser;

  @Autowired
  public DifyKnowledgeOrganizer(
      AiOrchestrationClient client,
      ObjectMapper mapper,
      AppProperties properties,
      AiExternalUserResolver externalUsers) {
    this(client, mapper, properties, externalUsers::resolve);
  }

  DifyKnowledgeOrganizer(
      AiOrchestrationClient client,
      ObjectMapper mapper,
      AppProperties properties,
      Supplier<String> externalUser) {
    this.client = client;
    this.mapper = mapper;
    this.maxItems = Math.max(1, properties.webImport().maxItems());
    this.externalUser = externalUser;
  }

  @Override
  public List<OrganizedKnowledge> organize(String sourceLabel, String text) {
    Map<String, String> inputs = new LinkedHashMap<>();
    inputs.put("operation", "knowledge_organize");
    inputs.put("sourceKind", safeLabel(sourceLabel));
    inputs.put("content", text == null ? "" : text);
    try {
      String result =
          client.run(AiOrchestrationWorkflow.ORGANIZER, Map.copyOf(inputs), externalUser.get());
      return OrganizedKnowledgeParser.parse(mapper, maxItems, result);
    } catch (AiOrchestrationException error) {
      throw new WebImportUnavailableException(
          "AI knowledge organization is temporarily unavailable.");
    }
  }

  private static String safeLabel(String value) {
    String normalized =
        value == null
            ? "unknown"
            : value.replaceAll("[\\p{Cc}]", " ").replaceAll("\\s+", " ").strip();
    if (normalized.isBlank()) {
      return "unknown";
    }
    return normalized.substring(0, Math.min(normalized.length(), 100));
  }
}

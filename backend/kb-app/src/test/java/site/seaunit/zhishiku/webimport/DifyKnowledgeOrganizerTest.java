package site.seaunit.zhishiku.webimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import site.seaunit.zhishiku.ai.orchestration.AiOrchestrationClient;
import site.seaunit.zhishiku.ai.orchestration.AiOrchestrationWorkflow;
import site.seaunit.zhishiku.config.AppProperties;

class DifyKnowledgeOrganizerTest {
  @Test
  void sendsRedactedTextToDifyAndParsesKnowledge() {
    AiOrchestrationClient client = mock(AiOrchestrationClient.class);
    when(client.run(eq(AiOrchestrationWorkflow.ORGANIZER), anyMap(), eq("account-1")))
        .thenReturn(
            """
            <think>内部推理不应进入知识。</think>
            {"items":[{"title":"识海架构","body":"## 核心边界\\n\\n后端负责业务数据。","summary":"说明后端与 Dify 的边界。","category":"架构","tags":["Dify","后端"]}]}
            """);
    DifyKnowledgeOrganizer organizer =
        new DifyKnowledgeOrganizer(client, new ObjectMapper(), properties(), () -> "account-1");

    List<OrganizedKnowledge> result = organizer.organize("web page", "脱敏后的网页正文");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).title()).isEqualTo("识海架构");
    assertThat(result.get(0).tags()).containsExactly("Dify", "后端");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> inputs = ArgumentCaptor.forClass(Map.class);
    verify(client).run(eq(AiOrchestrationWorkflow.ORGANIZER), inputs.capture(), eq("account-1"));
    assertThat(inputs.getValue())
        .containsEntry("operation", "knowledge_organize")
        .containsEntry("sourceKind", "web page")
        .containsEntry("content", "脱敏后的网页正文");
  }

  @Test
  void rejectsIncompleteDifyMetadata() {
    AiOrchestrationClient client = mock(AiOrchestrationClient.class);
    when(client.run(eq(AiOrchestrationWorkflow.ORGANIZER), anyMap(), eq("account-1")))
        .thenReturn(
            """
            {"items":[{"title":"缺少标签","body":"这是有效正文内容。","summary":"摘要","category":"分类","tags":[]}]}
            """);
    DifyKnowledgeOrganizer organizer =
        new DifyKnowledgeOrganizer(client, new ObjectMapper(), properties(), () -> "account-1");

    assertThatThrownBy(() -> organizer.organize("text", "有效文本"))
        .isInstanceOf(WebPageOrganizationException.class)
        .hasMessageContaining("incomplete");
  }

  private static AppProperties properties() {
    return new AppProperties(
        new AppProperties.Owner("", "", "", false),
        new AppProperties.Storage(".data/uploads"),
        new AppProperties.Security("zhishiku_session", "/kb"),
        new AppProperties.Captcha(120, 1000, false),
        new AppProperties.Registration(
            true, 600, 60, 3600, 5, 20, 5, 1000, false, "from@example.com", "识海"),
        new AppProperties.ExternalSearch("", "", "", "", 20, 8, 900),
        new AppProperties.Ai("http://127.0.0.1/chat", "test-key", "deepseek-chat", 20),
        new AppProperties.WebImport("", "tesseract", 35, 30, 30_000, 20));
  }
}

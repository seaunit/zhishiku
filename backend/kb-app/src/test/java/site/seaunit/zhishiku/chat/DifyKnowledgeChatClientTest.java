package site.seaunit.zhishiku.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import site.seaunit.zhishiku.ai.orchestration.AiOrchestrationClient;
import site.seaunit.zhishiku.ai.orchestration.AiOrchestrationWorkflow;
import site.seaunit.zhishiku.chat.KnowledgeChatModelClient.Evidence;

class DifyKnowledgeChatClientTest {
  @Test
  void delegatesKnowledgeAnswerAndPreservesCitationValidationData() {
    AiOrchestrationClient client = mock(AiOrchestrationClient.class);
    when(client.run(eq(AiOrchestrationWorkflow.CHAT), anyMap(), eq("account-1")))
        .thenReturn(
            """
            {"answer":"根据知识回答","usedSourceIds":["knowledge-1"],"needsInternet":false}
            """);
    DifyKnowledgeChatClient chat = new DifyKnowledgeChatClient(client, () -> "account-1");

    var answer =
        chat.answer("问题", List.of(), List.of(new Evidence("knowledge-1", "标题", "正文", null)));

    assertThat(answer.answer()).isEqualTo("根据知识回答");
    assertThat(answer.usedSourceIds()).containsExactly("knowledge-1");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> inputs = ArgumentCaptor.forClass(Map.class);
    verify(client).run(eq(AiOrchestrationWorkflow.CHAT), inputs.capture(), eq("account-1"));
    assertThat(inputs.getValue().get("operation")).isEqualTo("knowledge_answer");
    assertThat(inputs.getValue().get("evidence")).contains("[knowledge-1]");
  }

  @Test
  void delegatesGeneralAnswerAndQueryPlan() {
    AiOrchestrationClient client = mock(AiOrchestrationClient.class);
    when(client.run(eq(AiOrchestrationWorkflow.CHAT), anyMap(), eq("account-1")))
        .thenReturn("{\"answer\":\"你好，我是小识。\",\"needsInternet\":false}")
        .thenReturn("{\"queries\":[\"DeepSeek 禁令\",\"美国 出口限制\"]}");
    DifyKnowledgeChatClient chat = new DifyKnowledgeChatClient(client, () -> "account-1");

    var answer = chat.answerGenerally("你好", List.of());
    var queries = chat.planQueries("美国对 DeepSeek 的禁令是什么？");

    assertThat(answer.answer()).isEqualTo("你好，我是小识。");
    assertThat(queries).containsExactly("DeepSeek 禁令", "美国 出口限制");
  }

  @Test
  void streamsKnowledgeAnswerThroughDifyAdapter() {
    AiOrchestrationClient client = mock(AiOrchestrationClient.class);
    when(client.stream(
            eq(AiOrchestrationWorkflow.CHAT),
            anyMap(),
            eq("account-1"),
            org.mockito.ArgumentMatchers.any()))
        .thenAnswer(
            (Answer<String>)
                invocation -> {
                  @SuppressWarnings("unchecked")
                  java.util.function.Consumer<String> consumer = invocation.getArgument(3);
                  consumer.accept("回答");
                  consumer.accept(" [1]");
                  return "回答 [1]";
                });
    DifyKnowledgeChatClient chat = new DifyKnowledgeChatClient(client, () -> "account-1");
    List<String> tokens = new ArrayList<>();

    String result =
        chat.streamAnswer(
            "问题", List.of(), List.of(new Evidence("knowledge-1", "标题", "正文", null)), tokens::add);

    assertThat(result).isEqualTo("回答 [1]");
    assertThat(tokens).containsExactly("回答", " [1]");
  }

  @Test
  void doesNotCallDifyWhenKnowledgeEvidenceIsEmpty() {
    AiOrchestrationClient client = mock(AiOrchestrationClient.class);
    DifyKnowledgeChatClient chat = new DifyKnowledgeChatClient(client, () -> "account-1");

    var answer = chat.answer("问题", List.of(), List.of());
    String streamed = chat.streamAnswer("问题", List.of(), List.of(), ignored -> {});

    assertThat(answer.needsInternet()).isTrue();
    assertThat(streamed).isEqualTo("INSUFFICIENT_EVIDENCE");
    verify(client, never()).run(any(), anyMap(), any());
    verify(client, never()).stream(any(), anyMap(), any(), any());
  }
}

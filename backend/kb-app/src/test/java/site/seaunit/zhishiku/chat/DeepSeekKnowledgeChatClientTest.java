package site.seaunit.zhishiku.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import site.seaunit.zhishiku.webimport.WebImportUnavailableException;

class DeepSeekKnowledgeChatClientTest {
  @Test
  void parsesStructuredAnswerAndDeduplicatesReferences() {
    var answer =
        DeepSeekKnowledgeChatClient.parse(
            """
            ```json
            {"answer":"基于知识的回答","usedSourceIds":["a","a","b"],"needsInternet":false}
            ```
            """);

    assertThat(answer.answer()).isEqualTo("基于知识的回答");
    assertThat(answer.usedSourceIds()).containsExactly("a", "b");
    assertThat(answer.needsInternet()).isFalse();
  }

  @Test
  void blankAnswerRequiresInternetEvenWhenModelReturnsFalse() {
    var answer =
        DeepSeekKnowledgeChatClient.parse(
            "{\"answer\":\"\",\"usedSourceIds\":[],\"needsInternet\":false}");

    assertThat(answer.answer()).isEmpty();
    assertThat(answer.needsInternet()).isTrue();
  }

  @Test
  void parsesGeneralConversationWithoutCitations() {
    var answer =
        DeepSeekKnowledgeChatClient.parse(
            "{\"answer\":\"你好，我是小识。\",\"needsInternet\":false}");

    assertThat(answer.answer()).isEqualTo("你好，我是小识。");
    assertThat(answer.usedSourceIds()).isEmpty();
    assertThat(answer.needsInternet()).isFalse();
  }

  @Test
  void rejectsMalformedModelOutput() {
    assertThatThrownBy(() -> DeepSeekKnowledgeChatClient.parse("not-json"))
        .isInstanceOf(WebImportUnavailableException.class)
        .hasMessageContaining("malformed");
  }

  @Test
  void parsesAndDeduplicatesAiRetrievalPlan() {
    var queries =
        DeepSeekKnowledgeChatClient.parseQueries(
            """
            {"queries":["美国 DeepSeek 禁令","DeepSeek ban","禁令","禁令","美国 出口限制"]}
            """);

    assertThat(queries).containsExactly("美国 DeepSeek 禁令", "DeepSeek ban", "禁令", "美国 出口限制");
  }
}

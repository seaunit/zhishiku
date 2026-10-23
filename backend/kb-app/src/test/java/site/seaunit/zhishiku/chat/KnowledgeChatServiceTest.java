package site.seaunit.zhishiku.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import site.seaunit.zhishiku.billing.BillingService;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.AnswerRequest;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.AnswerResponse;
import site.seaunit.zhishiku.chat.KnowledgeChatModelClient.ModelAnswer;
import site.seaunit.zhishiku.chat.KnowledgeChatRetrievalService.RetrievedKnowledge;
import site.seaunit.zhishiku.consent.AiConsentService;
import site.seaunit.zhishiku.core.billing.BillingModel.Feature;
import site.seaunit.zhishiku.idempotency.IdempotencyService;
import site.seaunit.zhishiku.search.external.DeepSeekWebProvider;
import site.seaunit.zhishiku.webimport.AiTextSecretRedactor;

@ExtendWith(MockitoExtension.class)
class KnowledgeChatServiceTest {
  private static final String KEY = "chat-test-key-0001";
  private static final UUID TASK_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Mock private KnowledgeChatRetrievalService retrieval;
  @Mock private KnowledgeChatModelClient model;
  @Mock private DeepSeekWebProvider searchProvider;
  @Mock private BillingService billing;
  @Mock private AiConsentService consents;
  @Mock private IdempotencyService idempotency;
  @Mock private Authentication authentication;

  private KnowledgeChatService service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    service =
        new KnowledgeChatService(
            retrieval,
            model,
            searchProvider,
            billing,
            consents,
            idempotency,
            new AiTextSecretRedactor(),
            true);
    when(idempotency.replay(any(), anyString(), anyString(), anyString(), any(Class.class)))
        .thenReturn(Optional.empty());
  }

  @Test
  void answersGeneralConversationWithoutKnowledgeAndSettlesOnePoint() {
    when(retrieval.retrieve("问题", authentication)).thenReturn(List.of());
    when(model.answerGenerally("问题", List.of()))
        .thenReturn(new ModelAnswer("你好，我是小识。", List.of(), false));

    AnswerResponse response = service.answer(request(TASK_ID), KEY, authentication);

    assertThat(response.needsInternet()).isFalse();
    assertThat(response.answer()).contains("我是小识");
    assertThat(response.sources()).isEmpty();
    verify(model).answerGenerally("问题", List.of());
    verify(model, never()).answer(anyString(), any(), any());
    verify(billing).settle(authentication, TASK_ID, Set.of(Feature.AI_ANSWER), "General answer");
  }

  @Test
  void fallsBackToGeneralAiWhenRetrievedEvidenceCannotSupportAnAnswer() {
    RetrievedKnowledge source = knowledge();
    when(retrieval.retrieve("问题", authentication)).thenReturn(List.of(source));
    when(model.answer(anyString(), any(), any()))
        .thenReturn(new ModelAnswer("", List.of(), true));
    when(model.answerGenerally("问题", List.of()))
        .thenReturn(new ModelAnswer("这是一个通用的自然语言回答。", List.of(), false));

    AnswerResponse response = service.answer(request(TASK_ID), KEY, authentication);

    assertThat(response.answer()).contains("通用的自然语言回答");
    assertThat(response.sources()).isEmpty();
    verify(model).answerGenerally("问题", List.of());
    verify(billing).settle(authentication, TASK_ID, Set.of(Feature.AI_ANSWER), "General answer");
  }

  @Test
  void requestsInternetWhenBothKnowledgeAndGeneralAiCannotAnswer() {
    when(retrieval.retrieve("问题", authentication)).thenReturn(List.of());
    when(model.answerGenerally("问题", List.of()))
        .thenReturn(new ModelAnswer("", List.of(), true));

    AnswerResponse response = service.answer(request(TASK_ID), KEY, authentication);

    assertThat(response.needsInternet()).isTrue();
    assertThat(response.reason()).isEqualTo("knowledge_insufficient");
    verify(billing).releaseAfterFailure(authentication, TASK_ID, "No supported answer");
    verify(billing, never()).settle(any(Authentication.class), any(), any(), anyString());
  }

  @Test
  void settlesOneAnswerPointAndReturnsOnlyCitedKnowledge() {
    RetrievedKnowledge source = knowledge();
    when(retrieval.retrieve("问题", authentication)).thenReturn(List.of(source));
    when(model.answer(anyString(), any(), any()))
        .thenReturn(
            new ModelAnswer("这是一条有依据的回答。", List.of(source.knowledgeId().toString()), false));

    AnswerResponse response = service.answer(request(TASK_ID), KEY, authentication);

    assertThat(response.needsInternet()).isFalse();
    assertThat(response.answer()).contains("有依据");
    assertThat(response.sources()).hasSize(1);
    assertThat(response.sources().get(0).knowledgeId()).isEqualTo(source.knowledgeId());
    verify(billing).requireReserved(authentication, TASK_ID, "knowledge_chat");
    verify(billing).settle(authentication, TASK_ID, Set.of(Feature.AI_ANSWER), "Knowledge answer");
  }

  @Test
  void usesAiRetrievalPlanWhenTheInitialLexicalRecallIsEmpty() {
    RetrievedKnowledge source = knowledge();
    when(retrieval.retrieve("美国对DeepSeek的禁令是什么？", authentication)).thenReturn(List.of());
    when(retrieval.hasActiveKnowledge(authentication)).thenReturn(true);
    when(model.planQueries("美国对DeepSeek的禁令是什么？"))
        .thenReturn(List.of("美国 DeepSeek 禁令", "DeepSeek ban", "出口限制"));
    when(retrieval.retrieve(
            "美国对DeepSeek的禁令是什么？",
            List.of("美国 DeepSeek 禁令", "DeepSeek ban", "出口限制"),
            authentication))
        .thenReturn(List.of(source));
    when(model.answer(anyString(), any(), any()))
        .thenReturn(
            new ModelAnswer(
                "美国对 DeepSeek 发布了限制性禁令。", List.of(source.knowledgeId().toString()), false));

    AnswerResponse response =
        service.answer(
            new AnswerRequest("美国对DeepSeek的禁令是什么？", List.of(), true, TASK_ID), KEY, authentication);

    assertThat(response.answer()).contains("禁令");
    assertThat(response.sources()).hasSize(1);
    verify(model).planQueries("美国对DeepSeek的禁令是什么？");
    verify(billing).settle(authentication, TASK_ID, Set.of(Feature.AI_ANSWER), "Knowledge answer");
  }

  @Test
  void rejectsUnknownCitationsAndReleasesTheAnswerReservation() {
    RetrievedKnowledge source = knowledge();
    when(retrieval.retrieve("问题", authentication)).thenReturn(List.of(source));
    when(model.answer(anyString(), any(), any()))
        .thenReturn(new ModelAnswer("没有合法引用", List.of(UUID.randomUUID().toString()), false));

    AnswerResponse response = service.answer(request(TASK_ID), KEY, authentication);

    assertThat(response.needsInternet()).isTrue();
    assertThat(response.sources()).isEmpty();
    verify(billing).releaseAfterFailure(authentication, TASK_ID, "No supported answer");
    verify(billing, never()).settle(any(Authentication.class), any(), any(), anyString());
  }

  @Test
  void streamsAnswerTokensAndSettlesOnlyAfterValidCitation() {
    RetrievedKnowledge source = knowledge();
    when(retrieval.retrieve("问题", authentication)).thenReturn(List.of(source));
    doAnswer(
            invocation -> {
              Consumer<String> tokenConsumer = invocation.getArgument(3);
              tokenConsumer.accept("根据");
              tokenConsumer.accept("知识[1]。");
              return "根据知识[1]。";
            })
        .when(model)
        .streamAnswer(anyString(), any(), any(), any());

    StringBuilder visible = new StringBuilder();
    AnswerResponse response =
        service.streamAnswer(request(TASK_ID), KEY, authentication, visible::append);

    assertThat(visible).hasToString("根据知识[1]。");
    assertThat(response.answer()).isEqualTo("根据知识[1]。");
    assertThat(response.sources()).singleElement();
    verify(billing).settle(authentication, TASK_ID, Set.of(Feature.AI_ANSWER), "Knowledge answer");
  }

  @Test
  void releasesStreamReservationWhenAnswerHasNoValidCitation() {
    RetrievedKnowledge source = knowledge();
    when(retrieval.retrieve("问题", authentication)).thenReturn(List.of(source));
    doAnswer(
            invocation -> {
              Consumer<String> tokenConsumer = invocation.getArgument(3);
              tokenConsumer.accept("没有引用的回答");
              return "没有引用的回答";
            })
        .when(model)
        .streamAnswer(anyString(), any(), any(), any());

    AnswerResponse response =
        service.streamAnswer(request(TASK_ID), KEY, authentication, ignored -> {});

    assertThat(response.needsInternet()).isTrue();
    verify(billing).releaseAfterFailure(authentication, TASK_ID, "No supported streamed answer");
    verify(billing, never()).settle(any(Authentication.class), any(), any(), anyString());
  }

  @Test
  void streamsGeneralConversationWhenKnowledgeIsEmpty() {
    when(retrieval.retrieve("你好", authentication)).thenReturn(List.of());
    doAnswer(
            invocation -> {
              Consumer<String> tokenConsumer = invocation.getArgument(2);
              tokenConsumer.accept("你好，");
              tokenConsumer.accept("我是小识。");
              return "你好，我是小识。";
            })
        .when(model)
        .streamGeneralAnswer(anyString(), any(), any());

    StringBuilder visible = new StringBuilder();
    AnswerResponse response =
        service.streamAnswer(
            new AnswerRequest("你好", List.of(), true, TASK_ID),
            KEY,
            authentication,
            visible::append);

    assertThat(visible).hasToString("你好，我是小识。");
    assertThat(response.answer()).isEqualTo("你好，我是小识。");
    assertThat(response.sources()).isEmpty();
    verify(billing).settle(authentication, TASK_ID, Set.of(Feature.AI_ANSWER), "General answer");
  }

  @Test
  void releasesStreamReservationWhenGeneralAnswerNeedsInternet() {
    when(retrieval.retrieve("今天天气怎么样", authentication)).thenReturn(List.of());
    when(model.streamGeneralAnswer(anyString(), any(), any())).thenReturn("NEEDS_INTERNET");

    StringBuilder visible = new StringBuilder();
    AnswerResponse response =
        service.streamAnswer(
            new AnswerRequest("今天天气怎么样", List.of(), true, TASK_ID),
            KEY,
            authentication,
            visible::append);

    assertThat(visible).isEmpty();
    assertThat(response.needsInternet()).isTrue();
    verify(billing).releaseAfterFailure(authentication, TASK_ID, "No supported streamed answer");
    verify(billing, never()).settle(any(Authentication.class), any(), any(), anyString());
  }

  @Test
  void internetAnswerSettlesSearchAndAnswerWhenBothSucceed() {
    String url = "https://example.com/reference";
    when(searchProvider.search("问题", 5))
        .thenReturn(
            List.of(
                new DeepSeekWebProvider.SearchResult(
                    "参考页面", url, "摘要", "可用于回答的联网证据。", "Example", null, null, 1, 1, "完整页面正文")));
    when(model.answer(anyString(), any(), any()))
        .thenReturn(new ModelAnswer("联网回答", List.of(url), false));

    AnswerResponse response = service.answerWithInternet(request(TASK_ID), KEY, authentication);

    assertThat(response.answer()).isEqualTo("联网回答");
    assertThat(response.sources())
        .singleElement()
        .extracting(source -> source.url())
        .isEqualTo(url);
    verify(billing).requireReserved(authentication, TASK_ID, "internet_search_with_answer");
    verify(billing)
        .settle(
            authentication,
            TASK_ID,
            Set.of(Feature.INTERNET_SEARCH, Feature.AI_ANSWER),
            "Internet search and answer completed");
  }

  @Test
  void internetZeroResultsSettlesOnlyTheOnePointSearch() {
    when(searchProvider.search("问题", 5)).thenReturn(List.of());

    AnswerResponse response = service.answerWithInternet(request(TASK_ID), KEY, authentication);

    assertThat(response.reason()).isEqualTo("internet_no_results");
    verify(billing)
        .settle(
            authentication,
            TASK_ID,
            Set.of(Feature.INTERNET_SEARCH),
            "Internet search returned no results");
    verify(model, never()).answer(anyString(), any(), any());
  }

  private static AnswerRequest request(UUID billingTaskId) {
    return new AnswerRequest("问题", List.of(), true, billingTaskId);
  }

  private static RetrievedKnowledge knowledge() {
    return new RetrievedKnowledge(
        UUID.fromString("22222222-2222-2222-2222-222222222222"),
        "知识标题",
        "知识摘要",
        "知识正文",
        "分类",
        List.of("标签"),
        "manual",
        null,
        List.of("body"));
  }
}

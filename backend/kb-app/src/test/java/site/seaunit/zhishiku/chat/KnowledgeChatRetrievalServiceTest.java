package site.seaunit.zhishiku.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.ClassificationSummary;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeSummaryResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.PageInfo;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.SearchHit;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.SearchResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.SourceSummary;
import site.seaunit.zhishiku.knowledge.KnowledgeService;

@ExtendWith(MockitoExtension.class)
class KnowledgeChatRetrievalServiceTest {
  @Mock private KnowledgeService knowledgeService;
  @Mock private Authentication authentication;

  @Test
  void removesTrailingQuestionPhraseWhenTheWholeSentenceHasNoExactMatch() {
    UUID id = UUID.randomUUID();
    KnowledgeSummaryResponse summary =
        new KnowledgeSummaryResponse(
            id,
            "识海聊天验收标记",
            "识海聊天验收标记是海蓝七号。",
            "用于聊天验收的私有知识。",
            "succeeded",
            "active",
            new SourceSummary("manual", "manual", null, null),
            new ClassificationSummary(id, "验收", "applied", "user", null, null),
            List.of(),
            1,
            Instant.parse("2026-10-23T00:00:00Z"),
            Instant.parse("2026-10-23T00:00:00Z"));
    SearchResponse empty =
        new SearchResponse(
            "什么是识海聊天验收标记？", List.of(), null, false, new PageInfo(1, 5, 0, 1, java.util.Map.of()));
    SearchResponse found =
        new SearchResponse(
            "识海聊天验收标记",
            List.of(new SearchHit(summary, List.of("title", "body"), java.util.Map.of())),
            null,
            false,
            new PageInfo(1, 5, 1, 1, java.util.Map.of()));
    when(knowledgeService.search("什么是识海聊天验收标记", 1, 8, "all", authentication)).thenReturn(empty);
    when(knowledgeService.search("识海聊天验收标记", 1, 8, "all", authentication)).thenReturn(found);
    when(knowledgeService.detail(id, authentication))
        .thenReturn(
            new KnowledgeItemResponse(
                id,
                summary.title(),
                "识海聊天验收标记是海蓝七号。",
                null,
                1,
                "not_required",
                "",
                summary.summary(),
                "succeeded",
                "active",
                false,
                summary.source(),
                summary.category(),
                summary.tags(),
                1,
                summary.createdAt(),
                summary.updatedAt()));

    var result =
        new KnowledgeChatRetrievalService(knowledgeService)
            .retrieve("什么是识海聊天验收标记？", authentication);

    assertThat(result)
        .singleElement()
        .satisfies(
            source -> {
              assertThat(source.knowledgeId()).isEqualTo(id);
              assertThat(source.content()).contains("海蓝七号");
            });
  }

  @Test
  void buildsDeduplicatedFallbacksFromQuestionAndTokens() {
    assertThat(KnowledgeChatRetrievalService.retrievalQueries("请问 产品设计 有哪些 方法？"))
        .contains("请问 产品设计 有哪些 方法", "产品设计")
        .doesNotHaveDuplicates();
    assertThat(KnowledgeChatRetrievalService.retrievalQueries("什么是许愿式编程？")).contains("许愿式编程");
  }
}

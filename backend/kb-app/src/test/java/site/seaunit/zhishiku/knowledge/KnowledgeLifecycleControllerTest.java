package site.seaunit.zhishiku.knowledge;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.BulkRecycleResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemPage;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeSummaryResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.PageInfo;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.SourceSummary;
import site.seaunit.zhishiku.security.SecurityConfig;

@WebMvcTest(KnowledgeController.class)
@Import({SecurityConfig.class, site.seaunit.zhishiku.common.ApiExceptionHandler.class})
class KnowledgeLifecycleControllerTest {
  private static final UUID ITEM_ID = UUID.fromString("123e4567-e89b-42d3-a456-426614174701");
  private static final UUID SECOND_ITEM_ID =
      UUID.fromString("123e4567-e89b-42d3-a456-426614174702");
  private static final String IDEMPOTENCY_KEY = "bulk-recycle-controller-0001";

  @Autowired private MockMvc mvc;

  @MockBean private KnowledgeService service;
  @MockBean private AuthenticationManager authenticationManager;

  @Test
  @WithMockUser(username = "alice@example.com")
  void passesPageSizeAndSourceFilterToLibraryPagination() throws Exception {
    Instant now = Instant.parse("2026-10-22T00:00:00Z");
    when(service.list(eq(2), eq(10), eq("webpage"), eq("review"), any()))
        .thenReturn(
            new KnowledgeItemPage(
                List.of(
                    new KnowledgeSummaryResponse(
                        ITEM_ID,
                        "网页知识",
                        "摘要",
                        null,
                        "succeeded",
                        "active",
                        new SourceSummary("webpage", "url", "https://example.com", now),
                        null,
                        List.of(),
                        1,
                        now,
                        now)),
                "3",
                new PageInfo(2, 10, 24, 3, java.util.Map.of("all", 24L, "webpage", 12L))));

    mvc.perform(
            get("/api/v1/knowledge-items")
                .param("page", "2")
                .param("limit", "10")
                .param("source", "webpage")
                .param("status", "review"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.pageInfo.page").value(2))
        .andExpect(jsonPath("$.pageInfo.totalItems").value(24))
        .andExpect(jsonPath("$.pageInfo.sourceCounts.webpage").value(12));

    verify(service).list(eq(2), eq(10), eq("webpage"), eq("review"), any());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void listsOnlyTheAuthenticatedOwnersTrash() throws Exception {
    Instant now = Instant.parse("2026-10-22T00:00:00Z");
    when(service.listTrash(eq(100), any()))
        .thenReturn(
            List.of(
                new KnowledgeSummaryResponse(
                    ITEM_ID,
                    "回收的知识",
                    "摘要",
                    null,
                    "not_requested",
                    "recycled",
                    new SourceSummary("manual", "manual", null, null),
                    null,
                    List.of(),
                    1,
                    now,
                    now)));

    mvc.perform(get("/api/v1/knowledge-items/trash"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].lifecycleState").value("recycled"));
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void recyclesRestoresAndPurgesWithOwnerScopedCalls() throws Exception {
    mvc.perform(delete("/api/v1/knowledge-items/{itemId}", ITEM_ID).with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(post("/api/v1/knowledge-items/{itemId}/restore", ITEM_ID).with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(delete("/api/v1/knowledge-items/{itemId}/permanent", ITEM_ID).with(csrf()))
        .andExpect(status().isNoContent());

    verify(service).recycle(eq(ITEM_ID), any());
    verify(service).restore(eq(ITEM_ID), any());
    verify(service).purge(eq(ITEM_ID), any());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void bulkRecyclingDeduplicatesItemsBeforeCallingTheService() throws Exception {
    when(service.bulkRecycle(eq(List.of(ITEM_ID, SECOND_ITEM_ID)), eq(IDEMPOTENCY_KEY), any()))
        .thenReturn(new BulkRecycleResponse(2));

    mvc.perform(
            post("/api/v1/knowledge-items/bulk-recycle")
                .with(csrf())
                .header("Idempotency-Key", IDEMPOTENCY_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"itemIds":["%s","%s","%s"]}
                    """
                        .formatted(ITEM_ID, SECOND_ITEM_ID, ITEM_ID)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recycled").value(2));

    verify(service).bulkRecycle(eq(List.of(ITEM_ID, SECOND_ITEM_ID)), eq(IDEMPOTENCY_KEY), any());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void appliesTheSizeLimitAfterDeduplicatingItemIds() throws Exception {
    when(service.bulkRecycle(eq(List.of(ITEM_ID)), eq(IDEMPOTENCY_KEY), any()))
        .thenReturn(new BulkRecycleResponse(1));

    mvc.perform(
            post("/api/v1/knowledge-items/bulk-recycle")
                .with(csrf())
                .header("Idempotency-Key", IDEMPOTENCY_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"itemIds":[%s]}
                    """
                        .formatted(("\"" + ITEM_ID + "\",").repeat(101).replaceFirst(",$", ""))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recycled").value(1));

    verify(service).bulkRecycle(eq(List.of(ITEM_ID)), eq(IDEMPOTENCY_KEY), any());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void rejectsBulkRecyclingWithoutCsrf() throws Exception {
    mvc.perform(
            post("/api/v1/knowledge-items/bulk-recycle")
                .header("Idempotency-Key", IDEMPOTENCY_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"itemIds":["%s"]}
                    """
                        .formatted(ITEM_ID)))
        .andExpect(status().isForbidden());

    verify(service, never()).bulkRecycle(any(), any(), any());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void rejectsBulkRecyclingWithoutIdempotencyKey() throws Exception {
    mvc.perform(
            post("/api/v1/knowledge-items/bulk-recycle")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"itemIds":["%s"]}
                    """
                        .formatted(ITEM_ID)))
        .andExpect(status().isBadRequest());

    verify(service, never()).bulkRecycle(any(), any(), any());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void rejectsAnEmptyBulkRecycleRequest() throws Exception {
    mvc.perform(
            post("/api/v1/knowledge-items/bulk-recycle")
                .with(csrf())
                .header("Idempotency-Key", IDEMPOTENCY_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"itemIds":[]}
                    """))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

    verify(service, never()).bulkRecycle(any(), any(), any());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void rejectsBulkRecyclingMoreThanOneHundredDistinctItems() throws Exception {
    List<String> itemIds = new ArrayList<>();
    for (int index = 0; index < 101; index++) {
      itemIds.add("\"" + UUID.randomUUID() + "\"");
    }

    mvc.perform(
            post("/api/v1/knowledge-items/bulk-recycle")
                .with(csrf())
                .header("Idempotency-Key", IDEMPOTENCY_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"itemIds\":[" + String.join(",", itemIds) + "]}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

    verify(service, never()).bulkRecycle(any(), any(), any());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void savesUserClassificationAsLockedFields() throws Exception {
    mvc.perform(
            patch("/api/v1/knowledge-items/{itemId}/classification", ITEM_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"category":"人工智能","tags":["知识管理","大模型"]}
                    """))
        .andExpect(status().isOk());

    verify(service)
        .updateClassification(
            eq(ITEM_ID),
            org.mockito.ArgumentMatchers.argThat(
                request ->
                    request != null
                        && "人工智能".equals(request.category())
                        && request.tags().contains("知识管理")),
            any());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void confirmsAnAiResultReviewWithOwnerScopedServiceState() throws Exception {
    mvc.perform(post("/api/v1/knowledge-items/{itemId}/review", ITEM_ID).with(csrf()))
        .andExpect(status().isOk());

    verify(service).confirmReview(eq(ITEM_ID), any());
  }

  @Test
  void rejectsUnauthenticatedTrashAccess() throws Exception {
    mvc.perform(get("/api/v1/knowledge-items/trash")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void requiresCsrfForLifecycleMutations() throws Exception {
    mvc.perform(delete("/api/v1/knowledge-items/{itemId}", ITEM_ID))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/v1/knowledge-items/{itemId}/restore", ITEM_ID))
        .andExpect(status().isForbidden());
    mvc.perform(delete("/api/v1/knowledge-items/{itemId}/permanent", ITEM_ID))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/v1/knowledge-items/{itemId}/review", ITEM_ID))
        .andExpect(status().isForbidden());
  }
}

package site.seaunit.zhishiku.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class KnowledgeDtos {
  private KnowledgeDtos() {}

  public record BodySourceRequest(String kind, String sourceUrl, Instant capturedAt) {}

  public record CreateKnowledgeRequest(
      @NotBlank @Size(max = 300) String title,
      @NotNull JsonNode bodyDocument,
      @Size(max = 20_000) String note,
      BodySourceRequest bodySource) {}

  public record UpdateKnowledgeRequest(
      long expectedVersion,
      @NotBlank @Size(max = 300) String title,
      @NotNull JsonNode bodyDocument,
      @Size(max = 20_000) String note,
      BodySourceRequest bodySource,
      String conflictResolution) {
    public UpdateKnowledgeRequest {
      conflictResolution = conflictResolution == null ? "reject" : conflictResolution;
    }
  }

  public record UpdateClassificationRequest(
      @NotBlank @Size(max = 100) String category,
      @NotNull @Size(max = 8) List<@NotBlank @Size(max = 100) String> tags) {}

  public record ReanalyzeKnowledgeRequest(boolean externalAiConsent, UUID billingTaskId) {}

  public record BulkRecycleRequest(@NotNull @Size(min = 1, max = 100) List<@NotNull UUID> itemIds) {
    public BulkRecycleRequest {
      itemIds = itemIds == null ? null : List.copyOf(new LinkedHashSet<>(itemIds));
    }
  }

  public record BulkRecycleResponse(int recycled) {}

  public record ReviewAllResponse(int reviewed) {}

  public record KnowledgeItemResponse(
      UUID id,
      String title,
      String body,
      JsonNode bodyDocument,
      int bodySchemaVersion,
      String bodyMigrationState,
      String note,
      String summary,
      String processingStatus,
      String lifecycleState,
      boolean redactionApplied,
      SourceSummary source,
      ClassificationSummary category,
      List<ClassificationSummary> tags,
      long version,
      Instant createdAt,
      Instant updatedAt) {}

  public record KnowledgeSummaryResponse(
      UUID id,
      String title,
      String bodyPreview,
      String summary,
      String processingStatus,
      String lifecycleState,
      SourceSummary source,
      ClassificationSummary category,
      List<ClassificationSummary> tags,
      long version,
      Instant createdAt,
      Instant updatedAt) {}

  public record PageInfo(
      int page,
      int pageSize,
      long totalItems,
      int totalPages,
      Map<String, Long> sourceCounts) {}

  public record KnowledgeItemPage(
      List<KnowledgeSummaryResponse> items, String nextCursor, PageInfo pageInfo) {}

  public record SourceSummary(String kind, String inputMethod, String url, Instant capturedAt) {
    public static SourceSummary manual() {
      return new SourceSummary("manual", "manual", null, null);
    }
  }

  public record ClassificationSummary(
      UUID id, String name, String status, String origin, Double confidence, Instant lockedAt) {}

  public record SearchHit(
      KnowledgeSummaryResponse item,
      List<String> matchedFields,
      Map<String, List<String>> highlights) {}

  public record SearchResponse(
      String query,
      List<SearchHit> items,
      String nextCursor,
      boolean degraded,
      PageInfo pageInfo) {}
}

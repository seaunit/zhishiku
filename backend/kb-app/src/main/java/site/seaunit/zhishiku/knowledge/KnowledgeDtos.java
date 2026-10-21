package site.seaunit.zhishiku.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
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
      @Size(max = 20_000) String note) {}

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

  public record KnowledgeItemPage(List<KnowledgeSummaryResponse> items, String nextCursor) {}

  public record SourceSummary(String kind, String inputMethod, String url, Instant capturedAt) {}

  public record ClassificationSummary(
      UUID id, String name, String status, String origin, Double confidence, Instant lockedAt) {}

  public record SearchHit(
      KnowledgeSummaryResponse item,
      List<String> matchedFields,
      Map<String, List<String>> highlights) {}

  public record SearchResponse(
      String query, List<SearchHit> items, String nextCursor, boolean degraded) {}
}

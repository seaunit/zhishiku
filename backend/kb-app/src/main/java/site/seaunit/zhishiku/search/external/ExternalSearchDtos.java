package site.seaunit.zhishiku.search.external;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;

public final class ExternalSearchDtos {
  private ExternalSearchDtos() {}

  public record ExternalSearchRequest(
      @NotBlank @Size(max = 300) String query,
      boolean explicitConsent,
      boolean includeAnswer,
      UUID billingTaskId) {
    public ExternalSearchRequest(String query, boolean explicitConsent, boolean includeAnswer) {
      this(query, explicitConsent, includeAnswer, null);
    }
  }

  public record ExternalSearchCandidate(
      UUID candidateId,
      String title,
      String url,
      String snippet,
      String provider,
      boolean organizable,
      Instant expiresAt) {}

  public record ExternalSearchResponse(List<ExternalSearchCandidate> items, String answer) {
    public ExternalSearchResponse(List<ExternalSearchCandidate> items) {
      this(items, null);
    }
  }

  public record SaveExternalCandidateRequest(boolean externalAiConsent, UUID billingTaskId) {}

  public record ExternalSearchSaveResponse(List<KnowledgeItemResponse> items) {}
}

package site.seaunit.zhishiku.search.external;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ExternalSearchDtos {
  private ExternalSearchDtos() {}

  public record ExternalSearchRequest(
      @NotBlank @Size(max = 300) String query, boolean explicitConsent, boolean includeAnswer) {}

  public record ExternalSearchCandidate(
      UUID candidateId,
      String title,
      String url,
      String snippet,
      String provider,
      Instant expiresAt) {}

  public record ExternalSearchResponse(List<ExternalSearchCandidate> items) {}

  public record SaveExternalCandidateRequest(UUID billingTaskId) {}
}

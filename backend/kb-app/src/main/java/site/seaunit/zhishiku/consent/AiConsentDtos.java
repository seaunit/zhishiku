package site.seaunit.zhishiku.consent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AiConsentDtos {
  private AiConsentDtos() {}

  public record AiConsentResponse(
      String scope,
      String service,
      String contentType,
      boolean granted,
      String consentVersion,
      Instant updatedAt) {}

  public record UpdateAiConsentRequest(
      boolean granted, @NotBlank @Size(max = 100) String consentVersion) {}
}

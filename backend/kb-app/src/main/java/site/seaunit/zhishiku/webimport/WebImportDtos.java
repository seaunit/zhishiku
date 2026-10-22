package site.seaunit.zhishiku.webimport;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;

public final class WebImportDtos {
  private WebImportDtos() {}

  public record WebPageImportRequest(
      @NotBlank @Size(max = 2048) String url, boolean externalAiConsent, UUID billingTaskId) {
    public WebPageImportRequest(String url, boolean externalAiConsent) {
      this(url, externalAiConsent, null);
    }
  }

  public record WebPageImportResponse(List<KnowledgeItemResponse> items) {}
}

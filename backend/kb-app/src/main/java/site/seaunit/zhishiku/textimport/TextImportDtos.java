package site.seaunit.zhishiku.textimport;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;

public final class TextImportDtos {
  private TextImportDtos() {}

  public record TextImportRequest(
      @NotBlank @Size(max = 30_000) String text,
      boolean externalAiConsent,
      @Size(max = 20_000) String note,
      UUID billingTaskId) {
    public TextImportRequest(String text, boolean externalAiConsent, String note) {
      this(text, externalAiConsent, note, null);
    }
  }

  public record TextImportResponse(List<KnowledgeItemResponse> items) {}
}

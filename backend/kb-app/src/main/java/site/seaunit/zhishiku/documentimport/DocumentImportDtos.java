package site.seaunit.zhishiku.documentimport;

import java.util.List;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;

public final class DocumentImportDtos {
  private DocumentImportDtos() {}

  public record DocumentImportResponse(List<KnowledgeItemResponse> items) {}
}

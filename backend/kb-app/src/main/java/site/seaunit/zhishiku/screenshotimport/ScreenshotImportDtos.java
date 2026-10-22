package site.seaunit.zhishiku.screenshotimport;

import java.util.List;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;

public final class ScreenshotImportDtos {
  private ScreenshotImportDtos() {}

  public record ScreenshotImportResponse(List<KnowledgeItemResponse> items) {}
}

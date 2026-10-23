package site.seaunit.zhishiku.screenshotimport;

import java.util.List;
import site.seaunit.zhishiku.richtext.image.ImportImageCandidate;

public record ExtractedScreenshot(
    String text,
    String mediaType,
    String formatLabel,
    int characterCount,
    List<ImportImageCandidate> images) {
  public ExtractedScreenshot {
    images = images == null ? List.of() : List.copyOf(images);
  }

  public ExtractedScreenshot(
      String text, String mediaType, String formatLabel, int characterCount) {
    this(text, mediaType, formatLabel, characterCount, List.of());
  }
}

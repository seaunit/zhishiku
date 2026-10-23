package site.seaunit.zhishiku.documentimport;

import java.util.List;
import site.seaunit.zhishiku.richtext.image.ImportImageCandidate;

public record ExtractedDocument(
    String text, String mediaType, int characterCount, List<ImportImageCandidate> images) {
  public ExtractedDocument {
    images = images == null ? List.of() : List.copyOf(images);
  }

  public ExtractedDocument(String text, String mediaType, int characterCount) {
    this(text, mediaType, characterCount, List.of());
  }
}

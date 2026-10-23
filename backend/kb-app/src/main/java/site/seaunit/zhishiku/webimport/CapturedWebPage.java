package site.seaunit.zhishiku.webimport;

import java.util.List;
import site.seaunit.zhishiku.richtext.image.ImportImageCandidate;

public record CapturedWebPage(
    String requestedUrl, String finalUrl, String visibleText, List<ImportImageCandidate> images) {
  public CapturedWebPage {
    images = images == null ? List.of() : List.copyOf(images);
  }

  public CapturedWebPage(String requestedUrl, String finalUrl, String visibleText) {
    this(requestedUrl, finalUrl, visibleText, List.of());
  }
}

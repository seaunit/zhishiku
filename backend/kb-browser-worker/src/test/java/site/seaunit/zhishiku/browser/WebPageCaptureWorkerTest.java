package site.seaunit.zhishiku.browser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class WebPageCaptureWorkerTest {
  @Test
  void onlyAllowsPublicHttpNetworkTargets() {
    assertThat(WebPageCaptureWorker.targetsPublicHost("https://example.com/article")).isTrue();
    assertThat(WebPageCaptureWorker.targetsPublicHost("https://8.8.8.8/")).isTrue();
    assertThat(WebPageCaptureWorker.targetsPublicHost("http://127.0.0.1/")).isFalse();
    assertThat(WebPageCaptureWorker.targetsPublicHost("http://0.0.0.1/")).isFalse();
    assertThat(WebPageCaptureWorker.targetsPublicHost("http://10.0.0.1/")).isFalse();
    assertThat(WebPageCaptureWorker.targetsPublicHost("http://100.64.0.1/")).isFalse();
    assertThat(WebPageCaptureWorker.targetsPublicHost("http://169.254.169.254/")).isFalse();
    assertThat(WebPageCaptureWorker.targetsPublicHost("http://[::1]/")).isFalse();
    assertThat(WebPageCaptureWorker.targetsPublicHost("file:///etc/passwd")).isFalse();
  }

  @Test
  void selectsLargeContentImagesAndSkipsNavigationAndSmallAssets() {
    var selected =
        WebPageCaptureWorker.selectVisualElements(
            List.of(
                new WebPageCaptureWorker.VisualElement(
                    "navigation-logo", 0, 0, 420, 160, true, "logo.png"),
                new WebPageCaptureWorker.VisualElement(
                    "article-image", 120, 600, 640, 360, false, "article.png"),
                new WebPageCaptureWorker.VisualElement(
                    "sidebar-thumb", 1100, 680, 120, 120, true, "thumb.png"),
                new WebPageCaptureWorker.VisualElement(
                    "duplicate-image", 121, 601, 639, 359, false, "article.png")));

    assertThat(selected)
        .extracting(WebPageCaptureWorker.VisualElement::key)
        .containsExactly("article-image");
  }
}

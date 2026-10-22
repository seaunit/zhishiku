package site.seaunit.zhishiku.browser;

import static org.assertj.core.api.Assertions.assertThat;

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
}

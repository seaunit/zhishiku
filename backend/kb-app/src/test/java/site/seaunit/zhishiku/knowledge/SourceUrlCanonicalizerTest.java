package site.seaunit.zhishiku.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SourceUrlCanonicalizerTest {
  private final SourceUrlCanonicalizer canonicalizer = new SourceUrlCanonicalizer();

  @Test
  void normalizesHostPortFragmentTrailingSlashAndTrackingParameters() {
    assertThat(
            canonicalizer.canonicalize(
                "HTTPS://Example.COM:443/article/?utm_source=newsletter&b=2&a=1#section"))
        .isEqualTo("https://example.com/article?a=1&b=2");
  }

  @Test
  void preservesMeaningfulQueryParameters() {
    assertThat(canonicalizer.canonicalize("https://example.com/search?q=deepseek&page=2"))
        .isEqualTo("https://example.com/search?page=2&q=deepseek");
  }

  @Test
  void rejectsNonHttpAndCredentialBearingUrls() {
    assertThatThrownBy(() -> canonicalizer.canonicalize("ftp://example.com/article"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> canonicalizer.canonicalize("https://user:pass@example.com/article"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}

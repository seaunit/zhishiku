package site.seaunit.zhishiku.richtext.provenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RichTextSourcePolicyTest {
  private static final String HASH = "a".repeat(64);
  private static final Instant CAPTURED = Instant.parse("2026-10-21T10:00:00Z");
  private static final Instant CONVERTED = Instant.parse("2026-10-21T10:01:00Z");
  private final RichTextSourcePolicy policy = new RichTextSourcePolicy();

  @Test
  void normalizesPublicWebSourceWithoutFragmentOrCredentials() {
    NormalizedRichTextSource source =
        policy.normalize(
            command("webpage", "HTTPS://Example.COM:443/article?q=1#private"), report("source-v1"));

    assertThat(source.url()).isEqualTo("https://example.com/article?q=1");
    assertThat(source.host()).isEqualTo("example.com");
    assertThat(source.converterVersion()).isEqualTo("source-v1");
    assertThat(source.report().preservedFeatures()).containsExactly("heading", "link");
  }

  @Test
  void rejectsPseudoProtocolsCredentialsAndPrivateTargets() {
    for (String url :
        new String[] {
          "javascript:alert(1)",
          "data:text/html,unsafe",
          "file:///etc/passwd",
          "blob:https://example.com/id",
          "https://user:password@example.com/private",
          "http://127.0.0.1/admin",
          "http://10.0.0.1/admin",
          "http://[::1]/admin",
          "http://localhost/admin"
        }) {
      assertThatThrownBy(() -> policy.normalize(command("webpage", url), report("source-v1")))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  void requiresCaptureTimeForExternalSourcesAndUrlForWebpages() {
    assertThatThrownBy(
            () ->
                policy.normalize(
                    new RichTextSourceCommand(
                        "clipboard", null, null, CONVERTED, "clipboard-v1", HASH),
                    report("clipboard-v1")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("capturedAt");

    assertThatThrownBy(() -> policy.normalize(command("webpage", null), report("source-v1")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("URL");

    assertThatThrownBy(
            () ->
                policy.normalize(
                    new RichTextSourceCommand(
                        "clipboard", null, CONVERTED.plusSeconds(1), CONVERTED, "source-v1", HASH),
                    report("source-v1")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("capturedAt");
  }

  @Test
  void rejectsMalformedHashUnknownFeaturesAndConverterMismatch() {
    assertThatThrownBy(
            () ->
                policy.normalize(
                    new RichTextSourceCommand(
                        "native", null, null, CONVERTED, "native-v1", "secret"),
                    report("native-v1")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("contentHash");

    assertThatThrownBy(
            () ->
                policy.normalize(
                    command("native", null),
                    new RichTextConversionReport(
                        List.of("arbitrary_html"), List.of(), List.of(), List.of(), "source-v1")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("feature");

    assertThatThrownBy(() -> policy.normalize(command("native", null), report("different-version")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("converter");
  }

  private static RichTextSourceCommand command(String kind, String url) {
    return new RichTextSourceCommand(kind, url, CAPTURED, CONVERTED, "source-v1", HASH);
  }

  private static RichTextConversionReport report(String converterVersion) {
    return new RichTextConversionReport(
        List.of("heading", "link"),
        List.of("font"),
        List.of("iframe"),
        List.of("script"),
        converterVersion);
  }
}

package site.seaunit.zhishiku.richtext.provenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SourceProvenancePolicyTest {
  private static final Instant NOW = Instant.parse("2026-10-22T01:02:03Z");
  private final SourceProvenancePolicy policy =
      new SourceProvenancePolicy(Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void sanitizesPublicWebSourceAndStripsFragmentAndCredentialsAreRejected() {
    SourceProvenance source =
        policy.authorize(
            "webpage",
            "HTTPS://Example.COM/article?q=1#tracking-fragment",
            Instant.parse("2026-10-22T01:00:00Z"));

    assertThat(source.kind()).isEqualTo("webpage");
    assertThat(source.url()).isEqualTo("https://example.com/article?q=1");
    assertThat(source.host()).isEqualTo("example.com");
    assertThat(source.capturedAt()).isEqualTo(Instant.parse("2026-10-22T01:00:00Z"));
    assertThat(source.convertedAt()).isEqualTo(NOW);
    assertThat(source.converterVersion()).isEqualTo("richtext-java-v1");

    assertThatThrownBy(
            () ->
                policy.authorize(
                    "webpage",
                    "https://user:secret@example.com/article",
                    Instant.parse("2026-10-22T01:00:00Z")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("credentials");
  }

  @Test
  void rejectsMissingExternalCaptureTimesAndPrivateOrLocalTargets() {
    assertThatThrownBy(() -> policy.authorize("clipboard", null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("captured");

    assertThatThrownBy(
            () ->
                policy.authorize(
                    "webpage", "http://127.0.0.1/private", Instant.parse("2026-10-22T01:00:00Z")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("public");

    assertThatThrownBy(
            () ->
                policy.authorize(
                    "webpage", "http://localhost/private", Instant.parse("2026-10-22T01:00:00Z")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("public");

    assertThatThrownBy(
            () -> policy.authorize("internet_result", null, Instant.parse("2026-10-22T01:00:00Z")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("URL");
  }

  @Test
  void permitsNativeContentWithoutExternalCaptureMetadata() {
    SourceProvenance source = policy.authorize("native", null, null);

    assertThat(source.url()).isNull();
    assertThat(source.host()).isNull();
    assertThat(source.capturedAt()).isNull();
  }

  @Test
  void permitsAiTextSourcesWithCaptureMetadata() {
    SourceProvenance source = policy.authorize("text", null, Instant.parse("2026-10-22T01:00:00Z"));

    assertThat(source.kind()).isEqualTo("text");
    assertThat(source.capturedAt()).isEqualTo(Instant.parse("2026-10-22T01:00:00Z"));
  }
}

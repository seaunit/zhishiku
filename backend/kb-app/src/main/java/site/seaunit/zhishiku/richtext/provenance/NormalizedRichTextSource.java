package site.seaunit.zhishiku.richtext.provenance;

import java.time.Instant;
import java.util.UUID;

public record NormalizedRichTextSource(
    UUID sourceId,
    String kind,
    String url,
    String host,
    Instant capturedAt,
    Instant convertedAt,
    String converterVersion,
    String contentHash,
    RichTextConversionReport report) {

  public NormalizedRichTextSource withSourceId(UUID id) {
    return new NormalizedRichTextSource(
        id, kind, url, host, capturedAt, convertedAt, converterVersion, contentHash, report);
  }
}

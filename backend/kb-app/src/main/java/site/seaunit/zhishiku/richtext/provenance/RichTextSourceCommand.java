package site.seaunit.zhishiku.richtext.provenance;

import java.time.Instant;

public record RichTextSourceCommand(
    String kind,
    String sourceUrl,
    Instant capturedAt,
    Instant convertedAt,
    String converterVersion,
    String contentHash) {}

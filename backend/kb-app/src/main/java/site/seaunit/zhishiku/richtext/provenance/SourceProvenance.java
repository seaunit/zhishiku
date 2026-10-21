package site.seaunit.zhishiku.richtext.provenance;

import java.time.Instant;

public record SourceProvenance(
    String kind,
    String url,
    String host,
    Instant capturedAt,
    Instant convertedAt,
    String converterVersion) {}

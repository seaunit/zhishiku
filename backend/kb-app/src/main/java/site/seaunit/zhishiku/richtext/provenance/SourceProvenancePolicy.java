package site.seaunit.zhishiku.richtext.provenance;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SourceProvenancePolicy {
  public static final String CONVERTER_VERSION = "richtext-java-v1";

  private static final Set<String> SOURCE_KINDS =
      Set.of(
          "native",
          "clipboard",
          "text",
          "word",
          "google_docs",
          "webpage",
          "document",
          "screenshot",
          "legacy_text",
          "internet_result");
  private static final Set<String> EXTERNAL_KINDS =
      Set.of(
          "clipboard",
          "text",
          "word",
          "google_docs",
          "webpage",
          "document",
          "screenshot",
          "legacy_text",
          "internet_result");
  private static final Set<String> URL_REQUIRED_KINDS = Set.of("webpage", "internet_result");
  private static final Set<String> CONVERSION_FEATURES =
      Set.of(
          "paragraph",
          "heading",
          "bold",
          "italic",
          "underline",
          "strike",
          "list",
          "quote",
          "link",
          "code",
          "code_block",
          "table",
          "image",
          "font",
          "color",
          "iframe",
          "script",
          "form",
          "tracking_image");
  private static final Set<String> SAFE_FEATURES =
      Set.of(
          "paragraph",
          "heading",
          "bold",
          "italic",
          "underline",
          "strike",
          "list",
          "quote",
          "link",
          "code",
          "code_block",
          "table",
          "image");
  private static final Set<String> DOWNGRADABLE_FEATURES = Set.of("font", "color");
  private static final Pattern SHA256 = Pattern.compile("^[a-f0-9]{64}$");
  private static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(5);

  private final Clock clock;

  public SourceProvenancePolicy() {
    this(Clock.systemUTC());
  }

  @Autowired
  public SourceProvenancePolicy(Clock clock) {
    this.clock = clock;
  }

  public SourceProvenance authorize(String requestedKind, String sourceUrl, Instant capturedAt) {
    String kind = normalizeKind(requestedKind);
    String sanitizedUrl = sanitizeUrl(sourceUrl);
    Instant now = clock.instant();

    if (URL_REQUIRED_KINDS.contains(kind) && sanitizedUrl == null) {
      throw new IllegalArgumentException("A webpage source requires a public HTTP/HTTPS URL.");
    }
    if (EXTERNAL_KINDS.contains(kind) && capturedAt == null) {
      throw new IllegalArgumentException("External content requires a capturedAt timestamp.");
    }
    if (capturedAt != null && capturedAt.isAfter(now.plus(MAX_CLOCK_SKEW))) {
      throw new IllegalArgumentException("The content capture time cannot be in the future.");
    }

    URI uri = sanitizedUrl == null ? null : URI.create(sanitizedUrl);
    return new SourceProvenance(
        kind, sanitizedUrl, uri == null ? null : uri.getHost(), capturedAt, now, CONVERTER_VERSION);
  }

  public NormalizedRichTextSource authorize(
      RichTextSourceCommand command, RichTextConversionReport report) {
    if (command == null) {
      throw new IllegalArgumentException("Source metadata is required.");
    }
    if (command.convertedAt() == null) {
      throw new IllegalArgumentException("Source conversion time is required.");
    }
    if (command.converterVersion() == null || command.converterVersion().isBlank()) {
      throw new IllegalArgumentException("Source converter version is required.");
    }
    if (command.contentHash() == null || !SHA256.matcher(command.contentHash()).matches()) {
      throw new IllegalArgumentException("Source contentHash must be a SHA-256 hex value.");
    }
    validateReport(report, command.converterVersion());
    SourceProvenance authorized =
        authorize(command.kind(), command.sourceUrl(), command.capturedAt());
    SourceProvenance converted =
        new SourceProvenance(
            authorized.kind(),
            authorized.url(),
            authorized.host(),
            authorized.capturedAt(),
            command.convertedAt(),
            command.converterVersion());
    return new NormalizedRichTextSource(
        null,
        converted.kind(),
        converted.url(),
        converted.host(),
        converted.capturedAt(),
        converted.convertedAt(),
        converted.converterVersion(),
        command.contentHash(),
        report);
  }

  public String sanitizeUrl(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    if (raw.chars().anyMatch(character -> Character.isISOControl(character))) {
      throw new IllegalArgumentException("Source URL contains control characters.");
    }
    try {
      URI uri = new URI(raw.strip());
      String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
      String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
      if (!Set.of("http", "https").contains(scheme) || host.isBlank()) {
        throw new IllegalArgumentException("Source URL must be an absolute HTTP/HTTPS URL.");
      }
      if (uri.getUserInfo() != null) {
        throw new IllegalArgumentException("Source URL must not contain credentials.");
      }
      if (!isPublicHost(host)) {
        throw new IllegalArgumentException("Source URL must target a public host.");
      }
      int port = uri.getPort();
      if (("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) {
        port = -1;
      }
      return new URI(scheme, null, host, port, uri.getPath(), uri.getQuery(), null).toString();
    } catch (URISyntaxException error) {
      throw new IllegalArgumentException("Source URL is invalid.");
    }
  }

  private static void validateReport(
      RichTextConversionReport report, String expectedConverterVersion) {
    if (report == null
        || report.converterVersion() == null
        || report.converterVersion().isBlank()) {
      throw new IllegalArgumentException("A conversion report and converter version are required.");
    }
    if (!report.converterVersion().equals(expectedConverterVersion)) {
      throw new IllegalArgumentException(
          "Source and conversion report converter versions must match.");
    }
    validateFeatures(report.preservedFeatures(), SAFE_FEATURES);
    validateFeatures(
        report.downgradedFeatures(),
        Set.copyOf(
            java.util.stream.Stream.concat(SAFE_FEATURES.stream(), DOWNGRADABLE_FEATURES.stream())
                .toList()));
    validateFeatures(report.removedFeatures(), CONVERSION_FEATURES);
    validateFeatures(report.rejectedFeatures(), CONVERSION_FEATURES);
  }

  private static void validateFeatures(List<String> values, Set<String> allowed) {
    if (values == null || !allowed.containsAll(values)) {
      throw new IllegalArgumentException("Conversion report contains an unsupported feature name.");
    }
  }

  private static String normalizeKind(String requestedKind) {
    String kind = requestedKind == null ? "native" : requestedKind.strip().toLowerCase(Locale.ROOT);
    if (!SOURCE_KINDS.contains(kind)) {
      throw new IllegalArgumentException("Unsupported body source kind.");
    }
    return kind;
  }

  private static boolean isPublicHost(String host) {
    String normalized = host.toLowerCase(Locale.ROOT);
    if (normalized.equals("localhost")
        || normalized.endsWith(".localhost")
        || normalized.endsWith(".local")
        || normalized.endsWith(".internal")
        || normalized.endsWith(".lan")
        || normalized.endsWith(".home")) {
      return false;
    }
    String literal = normalized;
    if (literal.startsWith("[") && literal.endsWith("]")) {
      literal = literal.substring(1, literal.length() - 1);
    }
    if (!literal.matches("\\d{1,3}(?:\\.\\d{1,3}){3}") && !literal.contains(":")) {
      return true;
    }
    try {
      InetAddress address = InetAddress.getByName(literal);
      return !(address.isAnyLocalAddress()
          || address.isLoopbackAddress()
          || address.isLinkLocalAddress()
          || address.isSiteLocalAddress()
          || address.isMulticastAddress());
    } catch (Exception error) {
      return false;
    }
  }
}

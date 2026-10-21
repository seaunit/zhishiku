package site.seaunit.zhishiku.richtext.provenance;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RichTextSourcePolicy {
  private static final Set<String> KINDS =
      Set.of(
          "native",
          "clipboard",
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
          "word",
          "google_docs",
          "webpage",
          "document",
          "screenshot",
          "legacy_text",
          "internet_result");
  private static final Set<String> SAFE_FEATURES =
      Set.of(
          "heading",
          "link",
          "paragraph",
          "bold",
          "italic",
          "underline",
          "strike",
          "code",
          "codeBlock",
          "list",
          "bulletList",
          "orderedList",
          "blockquote",
          "table",
          "tableHeader",
          "image",
          "hardBreak");
  private static final Set<String> KNOWN_FEATURES =
      Set.of(
          "heading",
          "link",
          "paragraph",
          "bold",
          "italic",
          "underline",
          "strike",
          "code",
          "codeBlock",
          "list",
          "bulletList",
          "orderedList",
          "blockquote",
          "table",
          "tableHeader",
          "image",
          "hardBreak",
          "font",
          "color",
          "iframe",
          "script",
          "form",
          "tracking_image");

  public NormalizedRichTextSource normalize(
      RichTextSourceCommand command, RichTextConversionReport report) {
    if (command == null || report == null) {
      throw new IllegalArgumentException("Source command and conversion report are required.");
    }
    String kind =
        command.kind() == null ? "native" : command.kind().strip().toLowerCase(Locale.ROOT);
    if (!KINDS.contains(kind)) {
      throw new IllegalArgumentException("Unsupported body source kind.");
    }
    if (command.convertedAt() == null) {
      throw new IllegalArgumentException("convertedAt is required.");
    }
    if (command.capturedAt() != null && command.capturedAt().isAfter(command.convertedAt())) {
      throw new IllegalArgumentException("capturedAt cannot be after convertedAt.");
    }
    if (command.converterVersion() == null || command.converterVersion().isBlank()) {
      throw new IllegalArgumentException("converterVersion is required.");
    }
    if (command.contentHash() == null || !command.contentHash().matches("[a-f0-9]{64}")) {
      throw new IllegalArgumentException("contentHash must be a lowercase SHA-256 hash.");
    }
    if (EXTERNAL_KINDS.contains(kind) && command.capturedAt() == null) {
      throw new IllegalArgumentException("capturedAt is required for external sources.");
    }
    URI uri = null;
    if (command.sourceUrl() != null && !command.sourceUrl().isBlank()) {
      uri = sanitizeUri(command.sourceUrl());
    }
    if (Set.of("webpage", "internet_result").contains(kind) && uri == null) {
      throw new IllegalArgumentException("sourceUrl (URL) is required for webpage sources.");
    }
    validateFeatureList(report.preservedFeatures(), "preserved", FeatureScope.PRESERVED);
    validateFeatureList(report.downgradedFeatures(), "downgraded", FeatureScope.KNOWN);
    validateFeatureList(report.removedFeatures(), "removed", FeatureScope.KNOWN);
    validateFeatureList(report.rejectedFeatures(), "rejected", FeatureScope.KNOWN);
    if (!command.converterVersion().equals(report.converterVersion())) {
      throw new IllegalArgumentException("Source and report converter versions must match.");
    }
    return new NormalizedRichTextSource(
        UUID.randomUUID(),
        kind,
        uri == null ? null : uri.toString(),
        uri == null ? null : uri.getHost(),
        command.capturedAt(),
        command.convertedAt(),
        report.converterVersion(),
        command.contentHash(),
        report);
  }

  private static void validateFeatureList(List<String> features, String label, FeatureScope scope) {
    Set<String> allowed = scope == FeatureScope.PRESERVED ? SAFE_FEATURES : KNOWN_FEATURES;
    for (String feature : features) {
      if (feature == null || !allowed.contains(feature)) {
        throw new IllegalArgumentException("Unknown " + label + " conversion feature.");
      }
    }
  }

  private enum FeatureScope {
    PRESERVED,
    KNOWN
  }

  private static URI sanitizeUri(String raw) {
    if (raw.chars().anyMatch(Character::isISOControl)) {
      throw new IllegalArgumentException("Source URL must target a public host.");
    }
    try {
      URI uri = new URI(raw.strip());
      String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
      String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
      if (!Set.of("http", "https").contains(scheme)
          || host.isBlank()
          || uri.getUserInfo() != null
          || !isPublicHost(host)) {
        throw new IllegalArgumentException("Source URL must target a public host.");
      }
      int port = uri.getPort();
      if (("https".equals(scheme) && port == 443) || ("http".equals(scheme) && port == 80)) {
        port = -1;
      }
      return new URI(scheme, null, host, port, uri.getPath(), uri.getQuery(), null);
    } catch (URISyntaxException error) {
      throw new IllegalArgumentException("Source URL must target a public host.");
    }
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

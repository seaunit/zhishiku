package site.seaunit.zhishiku.knowledge;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SourceUrlCanonicalizer {
  private static final Set<String> TRACKING_PARAMETERS =
      Set.of(
          "fbclid",
          "gclid",
          "dclid",
          "msclkid",
          "mc_cid",
          "mc_eid",
          "_ga",
          "_gl",
          "igshid",
          "yclid",
          "spm",
          "ref_src");

  public String canonicalize(String rawUrl) {
    if (rawUrl == null || rawUrl.isBlank()) {
      throw new IllegalArgumentException("A source URL is required.");
    }
    if (rawUrl.chars().anyMatch(Character::isISOControl)) {
      throw new IllegalArgumentException("Source URL contains control characters.");
    }
    try {
      URI uri = new URI(rawUrl.strip());
      String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
      String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
      if (!Set.of("http", "https").contains(scheme)
          || host.isBlank()
          || uri.getUserInfo() != null) {
        throw new IllegalArgumentException("Source URL must be a public HTTP or HTTPS URL.");
      }
      int port = uri.getPort();
      if (("https".equals(scheme) && port == 443) || ("http".equals(scheme) && port == 80)) {
        port = -1;
      }
      String path = uri.getRawPath();
      if (path == null || path.isBlank()) {
        path = "/";
      } else if (path.length() > 1 && path.endsWith("/")) {
        path = path.substring(0, path.length() - 1);
      }
      String query = canonicalQuery(uri.getRawQuery());
      StringBuilder canonical = new StringBuilder(scheme).append("://").append(host);
      if (port >= 0) {
        canonical.append(':').append(port);
      }
      canonical.append(path);
      if (!query.isBlank()) {
        canonical.append('?').append(query);
      }
      return canonical.toString();
    } catch (URISyntaxException error) {
      throw new IllegalArgumentException("Source URL must be a valid HTTP or HTTPS URL.", error);
    }
  }

  private static String canonicalQuery(String rawQuery) {
    if (rawQuery == null || rawQuery.isBlank()) {
      return "";
    }
    List<String> retained = new ArrayList<>();
    for (String part : rawQuery.split("&")) {
      if (part.isBlank()) {
        continue;
      }
      int separator = part.indexOf('=');
      String name = separator < 0 ? part : part.substring(0, separator);
      String normalizedName = name.toLowerCase(Locale.ROOT);
      if (normalizedName.startsWith("utm_") || TRACKING_PARAMETERS.contains(normalizedName)) {
        continue;
      }
      retained.add(part);
    }
    retained.sort(Comparator.naturalOrder());
    return String.join("&", retained);
  }
}

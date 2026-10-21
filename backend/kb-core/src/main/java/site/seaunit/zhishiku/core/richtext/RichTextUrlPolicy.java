package site.seaunit.zhishiku.core.richtext;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;

public final class RichTextUrlPolicy {
  private RichTextUrlPolicy() {}

  public static String validateAndNormalize(String raw) {
    if (raw == null || !raw.equals(raw.strip()) || containsControlCharacter(raw)) {
      throw unsafeUrl();
    }
    try {
      URI uri = new URI(raw);
      String scheme = uri.getScheme();
      String host = uri.getHost();
      if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
          || host == null
          || uri.getUserInfo() != null
          || isPrivateOrLocalHost(host)) {
        throw unsafeUrl();
      }
      int port = uri.getPort();
      if (("http".equalsIgnoreCase(scheme) && port == 80)
          || ("https".equalsIgnoreCase(scheme) && port == 443)) {
        port = -1;
      }
      return new URI(
              scheme.toLowerCase(Locale.ROOT),
              null,
              host.toLowerCase(Locale.ROOT),
              port,
              uri.getRawPath(),
              uri.getRawQuery(),
              null)
          .toASCIIString();
    } catch (URISyntaxException | IllegalArgumentException error) {
      throw unsafeUrl();
    }
  }

  public static boolean isPrivateOrLocalHost(String host) {
    String value = host.toLowerCase(Locale.ROOT);
    if ("localhost".equals(value)
        || value.endsWith(".localhost")
        || value.endsWith(".local")
        || value.endsWith(".internal")
        || value.equals("::1")
        || value.startsWith("fc")
        || value.startsWith("fd")
        || value.startsWith("fe80:")) {
      return true;
    }
    if (value.matches("\\d{1,3}(?:\\.\\d{1,3}){3}")) {
      return isPrivateIpv4(value);
    }
    if (value.indexOf(':') >= 0) {
      try {
        InetAddress address = InetAddress.getByName(value);
        return address.isAnyLocalAddress()
            || address.isLoopbackAddress()
            || address.isLinkLocalAddress()
            || address.isSiteLocalAddress()
            || address.isMulticastAddress();
      } catch (UnknownHostException error) {
        return true;
      }
    }
    // Hostnames are validated syntactically without DNS requests.
    return false;
  }

  private static boolean isPrivateIpv4(String value) {
    String[] parts = value.split("\\.");
    int[] octets = new int[4];
    try {
      for (int index = 0; index < parts.length; index++) {
        octets[index] = Integer.parseInt(parts[index]);
        if (octets[index] < 0 || octets[index] > 255) {
          return true;
        }
      }
    } catch (NumberFormatException error) {
      return true;
    }
    int first = octets[0];
    int second = octets[1];
    return first == 0
        || first == 10
        || first == 127
        || (first == 100 && second >= 64 && second <= 127)
        || (first == 169 && second == 254)
        || (first == 172 && second >= 16 && second <= 31)
        || (first == 192 && second == 168)
        || (first == 198 && (second == 18 || second == 19))
        || first >= 224;
  }

  private static boolean containsControlCharacter(String value) {
    return value.codePoints().anyMatch(Character::isISOControl);
  }

  private static RichTextException unsafeUrl() {
    return new RichTextException(
        "UNSAFE_URL", "URLs must be public absolute HTTP or HTTPS URLs without credentials.");
  }
}

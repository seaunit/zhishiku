package site.seaunit.zhishiku.auth.registration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.IDN;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class EmailAddressPolicy {
  private static final Pattern LOCAL_PART = Pattern.compile("^[a-z0-9.!#$%&'*+/=?^_`{|}~-]+$");
  private static final Pattern DOMAIN_LABEL =
      Pattern.compile("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$");
  private final Set<String> blockedDomains;

  public EmailAddressPolicy(Set<String> blockedDomains) {
    this.blockedDomains =
        blockedDomains.stream()
            .map(EmailAddressPolicy::normalizeDomain)
            .collect(Collectors.toUnmodifiableSet());
  }

  public static EmailAddressPolicy fromResource(String resourcePath) {
    try (InputStream input =
        EmailAddressPolicy.class.getClassLoader().getResourceAsStream(resourcePath)) {
      if (input == null) {
        throw new IllegalStateException("Missing disposable email domain resource.");
      }
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
        Set<String> domains =
            reader
                .lines()
                .map(String::strip)
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .collect(Collectors.toUnmodifiableSet());
        return new EmailAddressPolicy(domains);
      }
    } catch (IOException error) {
      throw new IllegalStateException("Could not load disposable email domains.", error);
    }
  }

  public String normalizeAndValidate(String rawEmail) {
    if (rawEmail == null) {
      throw new InvalidEmailAddressException();
    }
    String email = rawEmail.strip().toLowerCase(Locale.ROOT);
    int separator = email.lastIndexOf('@');
    if (separator <= 0 || separator == email.length() - 1 || email.length() > 254) {
      throw new InvalidEmailAddressException();
    }
    String localPart = email.substring(0, separator);
    String domain = normalizeDomain(email.substring(separator + 1));
    if (localPart.length() > 64
        || !LOCAL_PART.matcher(localPart).matches()
        || !isValidDomain(domain)) {
      throw new InvalidEmailAddressException();
    }
    if (isBlocked(domain)) {
      throw new EmailDomainNotAllowedException();
    }
    return localPart + "@" + domain;
  }

  private boolean isBlocked(String domain) {
    return blockedDomains.stream()
        .anyMatch(blocked -> domain.equals(blocked) || domain.endsWith("." + blocked));
  }

  private static String normalizeDomain(String domain) {
    String cleaned = domain.strip().toLowerCase(Locale.ROOT);
    while (cleaned.endsWith(".")) {
      cleaned = cleaned.substring(0, cleaned.length() - 1);
    }
    try {
      return IDN.toASCII(cleaned, IDN.ALLOW_UNASSIGNED).toLowerCase(Locale.ROOT);
    } catch (IllegalArgumentException error) {
      throw new InvalidEmailAddressException();
    }
  }

  private static boolean isValidDomain(String domain) {
    if (domain.isBlank() || domain.length() > 253) {
      return false;
    }
    String[] labels = domain.split("\\.", -1);
    if (labels.length < 2) {
      return false;
    }
    for (String label : labels) {
      if (!DOMAIN_LABEL.matcher(label).matches()) {
        return false;
      }
    }
    return true;
  }
}

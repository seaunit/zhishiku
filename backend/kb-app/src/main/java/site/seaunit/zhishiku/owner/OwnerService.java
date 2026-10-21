package site.seaunit.zhishiku.owner;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.seaunit.zhishiku.config.AppProperties;

@Service
public class OwnerService implements ApplicationRunner {
  private static final int MAX_DISPLAY_NAME_CODE_POINTS = 40;

  private final OwnerAccountRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final AppProperties properties;

  public OwnerService(
      OwnerAccountRepository repository,
      PasswordEncoder passwordEncoder,
      AppProperties properties) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
    this.properties = properties;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (properties.owner().autoBootstrap()
        && properties.owner().email() != null
        && !properties.owner().email().isBlank()
        && properties.owner().password() != null
        && properties.owner().password().length() >= 8
        && repository.count() == 0) {
      createOwner(properties.owner().email(), properties.owner().password(), "zh-CN");
    }
  }

  @Transactional(readOnly = true)
  public Optional<OwnerAccountEntity> findByEmail(String email) {
    return repository.findByEmailNormalized(email.strip().toLowerCase());
  }

  @Transactional(readOnly = true)
  public boolean isRegistrationOpen() {
    return properties.registration().enabled();
  }

  @Transactional
  public OwnerAccountEntity createOwner(String email, String password, String locale) {
    if (password == null || password.length() < 8 || password.length() > 256) {
      throw new IllegalArgumentException("Password must be between 8 and 256 characters.");
    }
    String normalizedEmail = email.strip().toLowerCase();
    if (repository.existsByEmailNormalized(normalizedEmail)) {
      throw new site.seaunit.zhishiku.auth.registration.EmailAlreadyRegisteredException();
    }
    String normalizedLocale = "en".equals(locale) ? "en" : "zh-CN";
    return repository.save(
        OwnerAccountEntity.create(
            normalizedEmail, passwordEncoder.encode(password), normalizedLocale));
  }

  @Transactional
  public OwnerAccountEntity setup(
      String bootstrapToken, String email, String password, String locale) {
    if (repository.count() > 0) {
      throw new IllegalStateException("Owner is already initialized.");
    }
    String expected = properties.owner().bootstrapToken();
    if (expected == null
        || expected.isBlank()
        || bootstrapToken == null
        || !MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            bootstrapToken.getBytes(StandardCharsets.UTF_8))) {
      throw new BadCredentialsException("Bootstrap token is invalid.");
    }
    return createOwner(email, password, locale);
  }

  @Transactional
  public OwnerAccountEntity updateDisplayName(String email, String displayName) {
    String normalizedDisplayName = normalizeDisplayName(displayName);
    OwnerAccountEntity owner =
        repository
            .findByEmailNormalized(email.strip().toLowerCase())
            .orElseThrow(() -> new IllegalStateException("Owner account is unavailable."));
    owner.updateDisplayName(normalizedDisplayName);
    return repository.save(owner);
  }

  public static String normalizeDisplayName(String displayName) {
    if (displayName == null) {
      throw invalidDisplayName();
    }

    StringBuilder normalized = new StringBuilder(displayName.length());
    boolean pendingSpace = false;
    for (int offset = 0; offset < displayName.length(); ) {
      int codePoint = displayName.codePointAt(offset);
      offset += Character.charCount(codePoint);
      if (Character.isISOControl(codePoint)
          || isUnsafeFormatControl(codePoint)
          || codePoint == '<'
          || codePoint == '>') {
        throw invalidDisplayName();
      }
      if (isWhitespace(codePoint)) {
        if (!normalized.isEmpty()) {
          pendingSpace = true;
        }
        continue;
      }
      if (pendingSpace) {
        normalized.append(' ');
        pendingSpace = false;
      }
      normalized.appendCodePoint(codePoint);
    }

    int length = normalized.codePointCount(0, normalized.length());
    if (length < 1 || length > MAX_DISPLAY_NAME_CODE_POINTS) {
      throw invalidDisplayName();
    }
    return normalized.toString();
  }

  private static boolean isWhitespace(int codePoint) {
    return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
  }

  private static boolean isUnsafeFormatControl(int codePoint) {
    return Character.getType(codePoint) == Character.FORMAT
        && codePoint != 0x200C
        && codePoint != 0x200D;
  }

  private static IllegalArgumentException invalidDisplayName() {
    return new IllegalArgumentException(
        "Display name must contain 1 to 40 characters without control characters or angle brackets.");
  }
}

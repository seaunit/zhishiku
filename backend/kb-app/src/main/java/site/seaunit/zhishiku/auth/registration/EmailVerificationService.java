package site.seaunit.zhishiku.auth.registration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class EmailVerificationService {
  private final RegistrationEmailSender sender;
  private final EmailAddressPolicy emailPolicy;
  private final BooleanSupplier registrationOpen;
  private final Clock clock;
  private final Supplier<String> codeSupplier;
  private final Config config;
  private final SecureRandom secureRandom = new SecureRandom();
  private final Cache<String, CodeEntry> codes;
  private final Cache<String, SendWindow> emailWindows;
  private final Cache<String, SendWindow> ipWindows;

  public EmailVerificationService(
      RegistrationEmailSender sender,
      EmailAddressPolicy emailPolicy,
      BooleanSupplier registrationOpen,
      Clock clock,
      Supplier<String> codeSupplier,
      Config config) {
    this.sender = sender;
    this.emailPolicy = emailPolicy;
    this.registrationOpen = registrationOpen;
    this.clock = clock;
    this.codeSupplier = codeSupplier;
    this.config = config;
    this.codes =
        Caffeine.newBuilder()
            .maximumSize(config.maxCachedCodes())
            .expireAfterWrite(config.codeTtl())
            .build();
    this.emailWindows =
        Caffeine.newBuilder()
            .maximumSize(config.maxCachedCodes())
            .expireAfterWrite(config.sendWindow())
            .build();
    this.ipWindows =
        Caffeine.newBuilder()
            .maximumSize(config.maxCachedCodes())
            .expireAfterWrite(config.sendWindow())
            .build();
  }

  public synchronized EmailCodeIssued issueCode(String rawEmail, String ipAddress) {
    requireRegistrationOpen();
    String email = emailPolicy.normalizeAndValidate(rawEmail);
    Instant now = clock.instant();
    SendWindow emailWindow = activeWindow(emailWindows.getIfPresent(email), now);
    SendWindow ipWindow = activeWindow(ipWindows.getIfPresent(sanitizeIp(ipAddress)), now);

    enforceCooldown(emailWindow, now);
    enforceHourlyLimit(emailWindow, config.maxSendsPerEmail(), now);
    enforceHourlyLimit(ipWindow, config.maxSendsPerIp(), now);

    String code = codeSupplier.get();
    if (code == null || !code.matches("\\d{6}")) {
      throw new IllegalStateException(
          "Email verification code generator returned an invalid value.");
    }

    try {
      sender.send(email, code);
    } catch (EmailDeliveryUnavailableException error) {
      throw error;
    } catch (RuntimeException error) {
      throw new EmailDeliveryUnavailableException(error);
    }

    byte[] salt = new byte[16];
    secureRandom.nextBytes(salt);
    Instant expiresAt = now.plus(config.codeTtl());
    codes.put(email, new CodeEntry(hash(code, salt), salt, expiresAt, 0));
    emailWindows.put(email, emailWindow.sentAt(now));
    ipWindows.put(sanitizeIp(ipAddress), ipWindow.sentAt(now));
    return new EmailCodeIssued(
        config.codeTtl().toSeconds(), config.resendCooldown().toSeconds(), code);
  }

  public synchronized boolean consumeCode(String rawEmail, String code) {
    requireRegistrationOpen();
    String email = emailPolicy.normalizeAndValidate(rawEmail);
    CodeEntry entry = codes.getIfPresent(email);
    Instant now = clock.instant();
    if (entry == null
        || !now.isBefore(entry.expiresAt())
        || entry.attempts() >= config.maxAttempts()) {
      codes.invalidate(email);
      throw new EmailCodeInvalidException();
    }

    int attempts = entry.attempts() + 1;
    boolean matches =
        code != null && MessageDigest.isEqual(entry.hash(), hash(code.strip(), entry.salt()));
    if (!matches) {
      if (attempts >= config.maxAttempts()) {
        codes.invalidate(email);
      } else {
        codes.put(email, new CodeEntry(entry.hash(), entry.salt(), entry.expiresAt(), attempts));
      }
      throw new EmailCodeInvalidException();
    }

    codes.invalidate(email);
    return true;
  }

  public boolean isRegistrationOpen() {
    return registrationOpen.getAsBoolean();
  }

  private void requireRegistrationOpen() {
    if (!registrationOpen.getAsBoolean()) {
      throw new RegistrationClosedException();
    }
  }

  private void enforceCooldown(SendWindow window, Instant now) {
    if (window.lastSentAt() == null) {
      return;
    }
    Instant allowedAt = window.lastSentAt().plus(config.resendCooldown());
    if (now.isBefore(allowedAt)) {
      throw new RateLimitedException(secondsUntil(now, allowedAt));
    }
  }

  private void enforceHourlyLimit(SendWindow window, int limit, Instant now) {
    if (window.count() >= limit) {
      throw new RateLimitedException(
          secondsUntil(now, window.startedAt().plus(config.sendWindow())));
    }
  }

  private SendWindow activeWindow(SendWindow window, Instant now) {
    if (window == null || !now.isBefore(window.startedAt().plus(config.sendWindow()))) {
      return new SendWindow(now, null, 0);
    }
    return window;
  }

  private static long secondsUntil(Instant now, Instant target) {
    return Math.max(1, Duration.between(now, target).toSeconds());
  }

  private static String sanitizeIp(String ipAddress) {
    return ipAddress == null || ipAddress.isBlank() ? "unknown" : ipAddress.strip();
  }

  private static byte[] hash(String code, byte[] salt) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(salt);
      return digest.digest(code.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 is unavailable.", error);
    }
  }

  public record Config(
      Duration codeTtl,
      Duration resendCooldown,
      Duration sendWindow,
      int maxSendsPerEmail,
      int maxSendsPerIp,
      int maxAttempts,
      long maxCachedCodes) {
    public Config {
      if (resendCooldown.compareTo(Duration.ofSeconds(60)) < 0) {
        resendCooldown = Duration.ofSeconds(60);
      }
    }

    public Config(
        Duration codeTtl,
        Duration resendCooldown,
        Duration sendWindow,
        int maxSendsPerEmail,
        int maxSendsPerIp,
        int maxAttempts) {
      this(
          codeTtl,
          resendCooldown,
          sendWindow,
          maxSendsPerEmail,
          maxSendsPerIp,
          maxAttempts,
          10_000);
    }
  }

  private record CodeEntry(byte[] hash, byte[] salt, Instant expiresAt, int attempts) {}

  private record SendWindow(Instant startedAt, Instant lastSentAt, int count) {
    SendWindow sentAt(Instant sentAt) {
      return new SendWindow(startedAt, sentAt, count + 1);
    }
  }
}

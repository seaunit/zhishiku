package site.seaunit.zhishiku.auth.captcha;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

public class CaptchaService {
  private final CaptchaRenderer renderer;
  private final Clock clock;
  private final Duration ttl;
  private final Cache<String, CaptchaEntry> challenges;

  public CaptchaService(CaptchaRenderer renderer, Clock clock, Duration ttl, long maxEntries) {
    this.renderer = renderer;
    this.clock = clock;
    this.ttl = ttl;
    this.challenges = Caffeine.newBuilder().maximumSize(maxEntries).expireAfterWrite(ttl).build();
  }

  public CaptchaChallenge issue() {
    CaptchaRendered rendered = renderer.render();
    String captchaId = UUID.randomUUID().toString();
    challenges.put(
        captchaId, new CaptchaEntry(normalize(rendered.answer()), clock.instant().plus(ttl)));
    String image = "data:image/png;base64," + Base64.getEncoder().encodeToString(rendered.png());
    return new CaptchaChallenge(captchaId, image, ttl.toSeconds(), rendered.answer());
  }

  public boolean consume(String captchaId, String answer) {
    if (captchaId == null || captchaId.isBlank() || answer == null || answer.isBlank()) {
      return false;
    }
    CaptchaEntry entry = challenges.asMap().remove(captchaId);
    if (entry == null || !clock.instant().isBefore(entry.expiresAt())) {
      return false;
    }
    return MessageDigest.isEqual(
        entry.answer().getBytes(StandardCharsets.UTF_8),
        normalize(answer).getBytes(StandardCharsets.UTF_8));
  }

  private static String normalize(String value) {
    return value.strip().toUpperCase(Locale.ROOT);
  }

  private record CaptchaEntry(String answer, Instant expiresAt) {}
}

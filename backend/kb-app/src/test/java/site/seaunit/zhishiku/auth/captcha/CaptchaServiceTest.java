package site.seaunit.zhishiku.auth.captcha;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class CaptchaServiceTest {
  private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G'};

  @Test
  void acceptsCorrectCodeIgnoringCaseAndConsumesItOnce() {
    MutableClock clock = new MutableClock(Instant.parse("2026-10-21T00:00:00Z"));
    CaptchaService service =
        new CaptchaService(
            () -> new CaptchaRendered("A1b2C", PNG), clock, Duration.ofMinutes(2), 100);

    CaptchaChallenge challenge = service.issue();

    assertThat(service.consume(challenge.captchaId(), "a1B2c")).isTrue();
    assertThat(service.consume(challenge.captchaId(), "a1B2c")).isFalse();
  }

  @Test
  void rejectsExpiredCode() {
    MutableClock clock = new MutableClock(Instant.parse("2026-10-21T00:00:00Z"));
    CaptchaService service =
        new CaptchaService(
            () -> new CaptchaRendered("ABCDE", PNG), clock, Duration.ofMinutes(2), 100);
    CaptchaChallenge challenge = service.issue();

    clock.advance(Duration.ofMinutes(2));

    assertThat(service.consume(challenge.captchaId(), "ABCDE")).isFalse();
  }

  @Test
  void rejectsWrongCodeAndBurnsTheChallenge() {
    CaptchaService service =
        new CaptchaService(
            () -> new CaptchaRendered("ABCDE", PNG),
            Clock.fixed(Instant.parse("2026-10-21T00:00:00Z"), ZoneOffset.UTC),
            Duration.ofMinutes(2),
            100);
    CaptchaChallenge challenge = service.issue();

    assertThat(service.consume(challenge.captchaId(), "WRONG")).isFalse();
    assertThat(service.consume(challenge.captchaId(), "ABCDE")).isFalse();
  }

  @Test
  void returnsAnEmbeddablePngChallenge() {
    CaptchaService service =
        new CaptchaService(
            () -> new CaptchaRendered("ABCDE", PNG),
            Clock.fixed(Instant.parse("2026-10-21T00:00:00Z"), ZoneOffset.UTC),
            Duration.ofSeconds(120),
            100);

    CaptchaChallenge challenge = service.issue();

    assertThat(challenge.image()).startsWith("data:image/png;base64,");
    assertThat(challenge.expiresInSeconds()).isEqualTo(120);
    assertThat(challenge.captchaId()).isNotBlank();
  }

  private static final class MutableClock extends Clock {
    private Instant instant;

    private MutableClock(Instant instant) {
      this.instant = instant;
    }

    void advance(Duration duration) {
      instant = instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant;
    }
  }
}

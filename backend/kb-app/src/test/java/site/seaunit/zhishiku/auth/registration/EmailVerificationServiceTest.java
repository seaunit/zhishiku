package site.seaunit.zhishiku.auth.registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EmailVerificationServiceTest {
  private static final EmailAddressPolicy POLICY = new EmailAddressPolicy(Set.of("mailinator.com"));
  private static final EmailVerificationService.Config CONFIG =
      new EmailVerificationService.Config(
          Duration.ofMinutes(10), Duration.ofSeconds(60), Duration.ofHours(1), 5, 20, 5);

  @Test
  void sendsSixDigitCodeAndConsumesItOnce() {
    FakeSender sender = new FakeSender();
    EmailVerificationService service =
        service(sender, new MutableClock(Instant.parse("2026-10-21T00:00:00Z")), "123456");

    service.issueCode("owner@example.com", "127.0.0.1");

    assertThat(sender.sent()).containsExactly(new SentCode("owner@example.com", "123456"));
    assertThat(service.consumeCode("owner@example.com", "123456")).isTrue();
    assertThatThrownBy(() -> service.consumeCode("owner@example.com", "123456"))
        .isInstanceOf(EmailCodeInvalidException.class);
  }

  @Test
  void rejectsExpiredCode() {
    FakeSender sender = new FakeSender();
    MutableClock clock = new MutableClock(Instant.parse("2026-10-21T00:00:00Z"));
    EmailVerificationService service = service(sender, clock, "123456");
    service.issueCode("owner@example.com", "127.0.0.1");

    clock.advance(Duration.ofMinutes(10));

    assertThatThrownBy(() -> service.consumeCode("owner@example.com", "123456"))
        .isInstanceOf(EmailCodeInvalidException.class);
  }

  @Test
  void rejectsAfterFiveWrongAttempts() {
    FakeSender sender = new FakeSender();
    EmailVerificationService service =
        service(sender, new MutableClock(Instant.parse("2026-10-21T00:00:00Z")), "123456");
    service.issueCode("owner@example.com", "127.0.0.1");

    for (int attempt = 0; attempt < 5; attempt++) {
      assertThatThrownBy(() -> service.consumeCode("owner@example.com", "000000"))
          .isInstanceOf(EmailCodeInvalidException.class);
    }

    assertThatThrownBy(() -> service.consumeCode("owner@example.com", "123456"))
        .isInstanceOf(EmailCodeInvalidException.class);
  }

  @Test
  void enforcesResendCooldownAndInvalidatesOldCodeAfterResend() {
    FakeSender sender = new FakeSender();
    MutableClock clock = new MutableClock(Instant.parse("2026-10-21T00:00:00Z"));
    Deque<String> codes = new ArrayDeque<>(List.of("111111", "222222"));
    EmailVerificationService service =
        new EmailVerificationService(sender, POLICY, () -> true, clock, codes::removeFirst, CONFIG);

    service.issueCode("owner@example.com", "127.0.0.1");
    assertThatThrownBy(() -> service.issueCode("owner@example.com", "127.0.0.1"))
        .isInstanceOf(RateLimitedException.class);

    clock.advance(Duration.ofSeconds(60));
    service.issueCode("owner@example.com", "127.0.0.1");

    assertThatThrownBy(() -> service.consumeCode("owner@example.com", "111111"))
        .isInstanceOf(EmailCodeInvalidException.class);
    assertThat(service.consumeCode("owner@example.com", "222222")).isTrue();
  }

  @Test
  void enforcesMinimumSixtySecondResendCooldown() {
    FakeSender sender = new FakeSender();
    MutableClock clock = new MutableClock(Instant.parse("2026-10-21T00:00:00Z"));
    Deque<String> codes = new ArrayDeque<>(List.of("111111", "222222"));
    EmailVerificationService.Config fastConfig =
        new EmailVerificationService.Config(
            Duration.ofMinutes(10), Duration.ofSeconds(5), Duration.ofHours(1), 5, 20, 5);
    EmailVerificationService service =
        new EmailVerificationService(
            sender, POLICY, () -> true, clock, codes::removeFirst, fastConfig);

    service.issueCode("owner@example.com", "127.0.0.1");
    clock.advance(Duration.ofSeconds(59));
    assertThatThrownBy(() -> service.issueCode("owner@example.com", "127.0.0.1"))
        .isInstanceOf(RateLimitedException.class);

    clock.advance(Duration.ofSeconds(1));
    service.issueCode("owner@example.com", "127.0.0.1");
    assertThat(sender.sent()).hasSize(2);
  }

  @Test
  void closedRegistrationDoesNotSend() {
    FakeSender sender = new FakeSender();
    EmailVerificationService service =
        new EmailVerificationService(
            sender,
            POLICY,
            () -> false,
            new MutableClock(Instant.parse("2026-10-21T00:00:00Z")),
            () -> "123456",
            CONFIG);

    assertThatThrownBy(() -> service.issueCode("owner@example.com", "127.0.0.1"))
        .isInstanceOf(RegistrationClosedException.class);
    assertThat(sender.sent()).isEmpty();
  }

  @Test
  void smtpFailureDoesNotLeaveAUsableCode() {
    FakeSender sender = new FakeSender();
    sender.fail = true;
    EmailVerificationService service =
        service(sender, new MutableClock(Instant.parse("2026-10-21T00:00:00Z")), "123456");

    assertThatThrownBy(() -> service.issueCode("owner@example.com", "127.0.0.1"))
        .isInstanceOf(EmailDeliveryUnavailableException.class);
    assertThatThrownBy(() -> service.consumeCode("owner@example.com", "123456"))
        .isInstanceOf(EmailCodeInvalidException.class);
  }

  @Test
  void limitsEachEmailToFiveSuccessfulSendsPerWindow() {
    FakeSender sender = new FakeSender();
    MutableClock clock = new MutableClock(Instant.parse("2026-10-21T00:00:00Z"));
    Deque<String> codes =
        new ArrayDeque<>(List.of("111111", "222222", "333333", "444444", "555555", "666666"));
    EmailVerificationService service =
        new EmailVerificationService(sender, POLICY, () -> true, clock, codes::removeFirst, CONFIG);

    for (int send = 0; send < 5; send++) {
      service.issueCode("owner@example.com", "127.0.0.1");
      clock.advance(Duration.ofSeconds(60));
    }

    assertThatThrownBy(() -> service.issueCode("owner@example.com", "127.0.0.1"))
        .isInstanceOf(RateLimitedException.class);
  }

  @Test
  void limitsEachIpAcrossDifferentEmailAddresses() {
    FakeSender sender = new FakeSender();
    MutableClock clock = new MutableClock(Instant.parse("2026-10-21T00:00:00Z"));
    Deque<String> codes = new ArrayDeque<>(List.of("111111", "222222", "333333"));
    EmailVerificationService.Config ipLimited =
        new EmailVerificationService.Config(
            Duration.ofMinutes(10), Duration.ofSeconds(60), Duration.ofHours(1), 5, 2, 5);
    EmailVerificationService service =
        new EmailVerificationService(
            sender, POLICY, () -> true, clock, codes::removeFirst, ipLimited);

    service.issueCode("first@example.com", "127.0.0.1");
    service.issueCode("second@example.com", "127.0.0.1");

    assertThatThrownBy(() -> service.issueCode("third@example.com", "127.0.0.1"))
        .isInstanceOf(RateLimitedException.class);
  }

  private static EmailVerificationService service(
      RegistrationEmailSender sender, MutableClock clock, String code) {
    return new EmailVerificationService(sender, POLICY, () -> true, clock, () -> code, CONFIG);
  }

  private record SentCode(String email, String code) {}

  private static final class FakeSender implements RegistrationEmailSender {
    private final List<SentCode> messages = new ArrayList<>();
    private boolean fail;

    @Override
    public void send(String email, String code) {
      if (fail) {
        throw new EmailDeliveryUnavailableException();
      }
      messages.add(new SentCode(email, code));
    }

    List<SentCode> sent() {
      return messages;
    }
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

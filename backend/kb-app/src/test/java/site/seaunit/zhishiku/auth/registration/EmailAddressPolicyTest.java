package site.seaunit.zhishiku.auth.registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;

class EmailAddressPolicyTest {
  private final EmailAddressPolicy policy =
      new EmailAddressPolicy(Set.of("mailinator.com", "yopmail.com"));

  @Test
  void normalizesAddressAndSiteDomainBoundary() {
    assertThat(policy.normalizeAndValidate("  Owner@Example.COM. ")).isEqualTo("owner@example.com");
  }

  @Test
  void rejectsMailinatorAndItsSubdomains() {
    assertThatThrownBy(() -> policy.normalizeAndValidate("daniel.parkerca61598b@mailinator.com"))
        .isInstanceOf(EmailDomainNotAllowedException.class);
    assertThatThrownBy(() -> policy.normalizeAndValidate("user@inbox.MAILINATOR.com"))
        .isInstanceOf(EmailDomainNotAllowedException.class);
  }

  @Test
  void allowsSimilarLookingDomainsAndRandomLocalPartsOnNormalProviders() {
    assertThat(policy.normalizeAndValidate("user@notmailinator.com"))
        .isEqualTo("user@notmailinator.com");
    assertThat(policy.normalizeAndValidate("daniel.parkerca61598b@gmail.com"))
        .isEqualTo("daniel.parkerca61598b@gmail.com");
  }

  @Test
  void rejectsMalformedAddress() {
    assertThatThrownBy(() -> policy.normalizeAndValidate("not-an-email"))
        .isInstanceOf(InvalidEmailAddressException.class);
  }
}

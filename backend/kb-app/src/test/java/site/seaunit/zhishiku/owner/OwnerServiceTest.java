package site.seaunit.zhishiku.owner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import site.seaunit.zhishiku.auth.registration.EmailAlreadyRegisteredException;
import site.seaunit.zhishiku.config.AppProperties;

class OwnerServiceTest {
  private final OwnerAccountRepository repository = mock(OwnerAccountRepository.class);
  private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
  private final OwnerService service =
      new OwnerService(repository, passwordEncoder, properties(true));

  @Test
  void registrationRemainsOpenAfterAccountsExist() {
    assertThat(service.isRegistrationOpen()).isTrue();
  }

  @Test
  void createsMultipleAccountsWithDistinctEmails() {
    when(passwordEncoder.encode("correct-horse-battery")).thenReturn("hash");
    when(repository.save(any(OwnerAccountEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    OwnerAccountEntity first =
        service.createOwner("first@example.com", "correct-horse-battery", "zh-CN");
    OwnerAccountEntity second =
        service.createOwner("second@example.com", "correct-horse-battery", "en");

    assertThat(first.getId()).isNotEqualTo(second.getId());
    assertThat(second.getEmailNormalized()).isEqualTo("second@example.com");
    assertThat(second.getLocale()).isEqualTo("en");
  }

  @Test
  void rejectsDuplicateEmailRegardlessOfCaseAndWhitespace() {
    when(repository.existsByEmailNormalized("owner@example.com")).thenReturn(true);

    assertThatThrownBy(
            () -> service.createOwner("  Owner@Example.COM ", "correct-horse-battery", "zh-CN"))
        .isInstanceOf(EmailAlreadyRegisteredException.class);
  }

  @Test
  void acceptsEightCharacterPasswordAndRejectsSevenCharacters() {
    when(passwordEncoder.encode("password")).thenReturn("hash");
    when(repository.save(any(OwnerAccountEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    assertThat(service.createOwner("eight@example.com", "password", "zh-CN").getEmailNormalized())
        .isEqualTo("eight@example.com");
    assertThatThrownBy(() -> service.createOwner("seven@example.com", "seven77", "zh-CN"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static AppProperties properties(boolean registrationEnabled) {
    return new AppProperties(
        new AppProperties.Owner("", "", "", false),
        new AppProperties.Storage(".data/uploads"),
        new AppProperties.Security("zhishiku_session", "/kb"),
        new AppProperties.Captcha(120, 1000, false),
        new AppProperties.Registration(
            registrationEnabled,
            600,
            60,
            3600,
            5,
            20,
            5,
            1000,
            false,
            "no-reply@mail.seaunit.site",
            "识海"));
  }
}

package site.seaunit.zhishiku.auth.registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;
import site.seaunit.zhishiku.owner.OwnerService;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {
  @Mock private OwnerService ownerService;
  @Mock private EmailVerificationService verificationService;

  private RegistrationService registrationService;

  @BeforeEach
  void setUp() {
    when(verificationService.isRegistrationOpen()).thenReturn(true);
    registrationService =
        new RegistrationService(
            ownerService, verificationService, new EmailAddressPolicy(Set.of("mailinator.com")));
  }

  @Test
  void rejectsMismatchedPasswordBeforeConsumingEmailCode() {
    RegistrationService.RegisterCommand command =
        new RegistrationService.RegisterCommand(
            "owner@example.com", "correct-horse-battery", "different-password", "123456", "zh-CN");

    assertThatThrownBy(() -> registrationService.register(command))
        .isInstanceOf(PasswordConfirmationMismatchException.class);

    verify(verificationService, never()).consumeCode("owner@example.com", "123456");
    verify(ownerService, never()).createOwner("owner@example.com", command.password(), "zh-CN");
  }

  @Test
  void createsAnAccountOnlyAfterEmailCodeIsConsumed() {
    RegistrationService.RegisterCommand command =
        new RegistrationService.RegisterCommand(
            "  Owner@Example.COM. ",
            "correct-horse-battery",
            "correct-horse-battery",
            "123456",
            "zh-CN");
    OwnerAccountEntity owner = OwnerAccountEntity.create("owner@example.com", "hash", "zh-CN");
    when(verificationService.consumeCode("owner@example.com", "123456")).thenReturn(true);
    when(ownerService.findByEmail("owner@example.com")).thenReturn(Optional.empty());
    when(ownerService.createOwner("owner@example.com", "correct-horse-battery", "zh-CN"))
        .thenReturn(owner);

    assertThat(registrationService.register(command)).isEqualTo("owner@example.com");

    verify(verificationService).consumeCode("owner@example.com", "123456");
    verify(ownerService).createOwner("owner@example.com", "correct-horse-battery", "zh-CN");
  }

  @Test
  void mapsConcurrentDuplicateRegistrationToEmailAlreadyRegistered() {
    RegistrationService.RegisterCommand command =
        new RegistrationService.RegisterCommand(
            "owner@example.com",
            "correct-horse-battery",
            "correct-horse-battery",
            "123456",
            "zh-CN");
    when(verificationService.consumeCode("owner@example.com", "123456")).thenReturn(true);
    when(ownerService.findByEmail("owner@example.com")).thenReturn(Optional.empty());
    when(ownerService.createOwner("owner@example.com", "correct-horse-battery", "zh-CN"))
        .thenThrow(new DataIntegrityViolationException("duplicate email"));

    assertThatThrownBy(() -> registrationService.register(command))
        .isInstanceOf(EmailAlreadyRegisteredException.class);
  }

  @Test
  void rejectsAnEmailThatAlreadyHasAnAccountBeforeSendingAnotherCode() {
    OwnerAccountEntity existing = OwnerAccountEntity.create("owner@example.com", "hash", "zh-CN");
    when(ownerService.findByEmail("owner@example.com")).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> registrationService.issueEmailCode("owner@example.com", "127.0.0.1"))
        .isInstanceOf(EmailAlreadyRegisteredException.class);
    verify(verificationService, never()).issueCode("owner@example.com", "127.0.0.1");
  }
}

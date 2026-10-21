package site.seaunit.zhishiku.auth.registration;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import site.seaunit.zhishiku.owner.OwnerService;

@Service
public class RegistrationService {
  private final OwnerService ownerService;
  private final EmailVerificationService verificationService;
  private final EmailAddressPolicy emailPolicy;

  public RegistrationService(
      OwnerService ownerService,
      EmailVerificationService verificationService,
      EmailAddressPolicy emailPolicy) {
    this.ownerService = ownerService;
    this.verificationService = verificationService;
    this.emailPolicy = emailPolicy;
  }

  public boolean isRegistrationOpen() {
    return ownerService.isRegistrationOpen() && verificationService.isRegistrationOpen();
  }

  public EmailCodeIssued issueEmailCode(String email, String ipAddress) {
    if (!isRegistrationOpen()) {
      throw new RegistrationClosedException();
    }
    return verificationService.issueCode(email, ipAddress);
  }

  public String register(RegisterCommand command) {
    if (!isRegistrationOpen()) {
      throw new RegistrationClosedException();
    }
    if (!command.password().equals(command.confirmPassword())) {
      throw new PasswordConfirmationMismatchException();
    }
    String email = emailPolicy.normalizeAndValidate(command.email());
    verificationService.consumeCode(email, command.emailCode());
    try {
      ownerService.createOwner(email, command.password(), command.locale());
      return email;
    } catch (IllegalStateException | DataIntegrityViolationException error) {
      throw new RegistrationClosedException();
    }
  }

  public record RegisterCommand(
      String email, String password, String confirmPassword, String emailCode, String locale) {}
}

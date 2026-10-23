package site.seaunit.zhishiku.auth.registration;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.seaunit.zhishiku.knowledge.StarterKnowledgeService;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;
import site.seaunit.zhishiku.owner.OwnerService;

@Service
public class RegistrationService {
  private final OwnerService ownerService;
  private final EmailVerificationService verificationService;
  private final EmailAddressPolicy emailPolicy;
  private final StarterKnowledgeService starterKnowledge;

  public RegistrationService(
      OwnerService ownerService,
      EmailVerificationService verificationService,
      EmailAddressPolicy emailPolicy,
      StarterKnowledgeService starterKnowledge) {
    this.ownerService = ownerService;
    this.verificationService = verificationService;
    this.emailPolicy = emailPolicy;
    this.starterKnowledge = starterKnowledge;
  }

  public boolean isRegistrationOpen() {
    return verificationService.isRegistrationOpen();
  }

  public EmailCodeIssued issueEmailCode(String email, String ipAddress) {
    if (!isRegistrationOpen()) {
      throw new RegistrationClosedException();
    }
    String normalizedEmail = emailPolicy.normalizeAndValidate(email);
    if (ownerService.findByEmail(normalizedEmail).isPresent()) {
      throw new EmailAlreadyRegisteredException();
    }
    return verificationService.issueCode(normalizedEmail, ipAddress);
  }

  @Transactional
  public String register(RegisterCommand command) {
    if (!isRegistrationOpen()) {
      throw new RegistrationClosedException();
    }
    if (!command.password().equals(command.confirmPassword())) {
      throw new PasswordConfirmationMismatchException();
    }
    String email = emailPolicy.normalizeAndValidate(command.email());
    if (ownerService.findByEmail(email).isPresent()) {
      throw new EmailAlreadyRegisteredException();
    }
    verificationService.consumeCode(email, command.emailCode());
    OwnerAccountEntity owner;
    try {
      owner = ownerService.createOwner(email, command.password(), command.locale());
    } catch (DataIntegrityViolationException error) {
      throw new EmailAlreadyRegisteredException();
    }
    starterKnowledge.seedForOwner(owner);
    return email;
  }

  public record RegisterCommand(
      String email, String password, String confirmPassword, String emailCode, String locale) {}
}

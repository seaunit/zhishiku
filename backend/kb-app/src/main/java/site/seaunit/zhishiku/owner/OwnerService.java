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
        && properties.owner().password().length() >= 12
        && repository.count() == 0) {
      createOwner(properties.owner().email(), properties.owner().password(), "zh-CN");
    }
  }

  @Transactional(readOnly = true)
  public OwnerAccountEntity requireOwner() {
    return repository
        .findByInstanceKey((short) 1)
        .orElseThrow(() -> new BadCredentialsException("Owner is not initialized."));
  }

  @Transactional(readOnly = true)
  public Optional<OwnerAccountEntity> findByEmail(String email) {
    return repository.findByEmailNormalized(email.strip().toLowerCase());
  }

  @Transactional
  public OwnerAccountEntity createOwner(String email, String password, String locale) {
    if (repository.count() > 0) {
      throw new IllegalStateException("Owner is already initialized.");
    }
    if (password == null || password.length() < 12 || password.length() > 256) {
      throw new IllegalArgumentException("Password must be between 12 and 256 characters.");
    }
    String normalizedLocale = "en".equals(locale) ? "en" : "zh-CN";
    return repository.save(
        OwnerAccountEntity.create(email, passwordEncoder.encode(password), normalizedLocale));
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
}

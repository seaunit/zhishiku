package site.seaunit.zhishiku.owner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import site.seaunit.zhishiku.persistence.AuditedEntity;

@Entity
@Table(name = "owner_account")
public class OwnerAccountEntity extends AuditedEntity {
  public static final String ADMIN_USERNAME = "a115846";

  @Id private UUID id;

  @Column(name = "email_normalized", nullable = false, unique = true)
  private String emailNormalized;

  @Column private String username;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(nullable = false)
  private String locale;

  @Column(name = "email_verified_at", nullable = false)
  private Instant emailVerifiedAt;

  @Column(name = "account_role", nullable = false)
  private String accountRole;

  @Column(name = "account_status", nullable = false)
  private String accountStatus;

  @Version
  @Column(nullable = false)
  private long version = 1;

  protected OwnerAccountEntity() {}

  public static OwnerAccountEntity create(String email, String passwordHash, String locale) {
    OwnerAccountEntity owner = new OwnerAccountEntity();
    owner.id = UUID.randomUUID();
    owner.emailNormalized = email.strip().toLowerCase();
    owner.displayName = defaultDisplayName(owner.emailNormalized);
    owner.passwordHash = passwordHash;
    owner.locale = locale;
    owner.emailVerifiedAt = Instant.now();
    owner.accountRole = "USER";
    owner.accountStatus = "enabled";
    return owner;
  }

  public static OwnerAccountEntity createAdmin(
      String username, String email, String passwordHash, String locale) {
    OwnerAccountEntity owner = create(email, passwordHash, locale);
    owner.username = normalizeUsername(username);
    owner.displayName = "识海管理员";
    owner.accountRole = "ADMIN";
    owner.accountStatus = "enabled";
    return owner;
  }

  public UUID getId() {
    return id;
  }

  public String getEmailNormalized() {
    return emailNormalized;
  }

  public String getUsername() {
    return username;
  }

  public String getLoginName() {
    return username == null || username.isBlank() ? emailNormalized : username;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void updateDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getLocale() {
    return locale;
  }

  public Instant getEmailVerifiedAt() {
    return emailVerifiedAt;
  }

  public String getAccountRole() {
    return accountRole;
  }

  public String getAccountStatus() {
    return accountStatus;
  }

  public boolean isAdmin() {
    return "ADMIN".equals(accountRole);
  }

  public boolean isEnabled() {
    return "enabled".equals(accountStatus);
  }

  public void updateEnabled(boolean enabled) {
    this.accountStatus = enabled ? "enabled" : "disabled";
  }

  public void updatePasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public void ensureAdministrator() {
    this.accountRole = "ADMIN";
    this.accountStatus = "enabled";
  }

  public static String normalizeUsername(String username) {
    if (username == null) {
      throw new IllegalArgumentException("Username is required.");
    }
    String normalized = username.strip();
    if (!normalized.matches("^[A-Za-z0-9]{1,64}$")) {
      throw new IllegalArgumentException("Username must contain 1 to 64 letters or digits.");
    }
    return normalized;
  }

  private static String defaultDisplayName(String emailNormalized) {
    int separator = emailNormalized.indexOf('@');
    String localPart = separator < 0 ? emailNormalized : emailNormalized.substring(0, separator);
    localPart = localPart.strip();
    if (localPart.isEmpty()) {
      localPart = "user";
    }
    int codePoints = localPart.codePointCount(0, localPart.length());
    if (codePoints <= 40) {
      return localPart;
    }
    return localPart.substring(0, localPart.offsetByCodePoints(0, 40));
  }
}

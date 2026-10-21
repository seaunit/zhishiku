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
  @Id private UUID id;

  @Column(name = "email_normalized", nullable = false, unique = true)
  private String emailNormalized;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(nullable = false)
  private String locale;

  @Column(name = "email_verified_at", nullable = false)
  private Instant emailVerifiedAt;

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
    return owner;
  }

  public UUID getId() {
    return id;
  }

  public String getEmailNormalized() {
    return emailNormalized;
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

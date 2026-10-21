package site.seaunit.zhishiku.owner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;
import site.seaunit.zhishiku.persistence.AuditedEntity;

@Entity
@Table(name = "owner_account")
public class OwnerAccountEntity extends AuditedEntity {
  @Id private UUID id;

  @Column(name = "instance_key", nullable = false, unique = true)
  private short instanceKey;

  @Column(name = "email_normalized", nullable = false, unique = true)
  private String emailNormalized;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(nullable = false)
  private String locale;

  @Version
  @Column(nullable = false)
  private long version = 1;

  protected OwnerAccountEntity() {}

  public static OwnerAccountEntity create(String email, String passwordHash, String locale) {
    OwnerAccountEntity owner = new OwnerAccountEntity();
    owner.id = UUID.randomUUID();
    owner.instanceKey = 1;
    owner.emailNormalized = email.strip().toLowerCase();
    owner.passwordHash = passwordHash;
    owner.locale = locale;
    return owner;
  }

  public UUID getId() {
    return id;
  }

  public String getEmailNormalized() {
    return emailNormalized;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getLocale() {
    return locale;
  }
}

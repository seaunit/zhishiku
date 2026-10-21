package site.seaunit.zhishiku.knowledge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;
import site.seaunit.zhishiku.persistence.AuditedEntity;

@Entity
@Table(name = "knowledge_item")
public class KnowledgeItemEntity extends AuditedEntity {
  @Id private UUID id;

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String body;

  @Column(nullable = false)
  private String note;

  @Column(name = "processing_status", nullable = false)
  private String processingStatus;

  @Column(name = "lifecycle_state", nullable = false)
  private String lifecycleState;

  @Column(name = "body_migration_state", nullable = false)
  private String bodyMigrationState;

  @Column(name = "search_revision", nullable = false)
  private long searchRevision;

  @Version
  @Column(nullable = false)
  private long version = 1;

  protected KnowledgeItemEntity() {}

  public static KnowledgeItemEntity create(UUID ownerId, String title, String body, String note) {
    KnowledgeItemEntity item = new KnowledgeItemEntity();
    item.id = UUID.randomUUID();
    item.ownerId = ownerId;
    item.title = title.strip();
    item.body = body;
    item.note = note == null ? "" : note.strip();
    item.processingStatus = "not_requested";
    item.lifecycleState = "active";
    item.bodyMigrationState = "completed";
    item.searchRevision = 1;
    return item;
  }

  public void update(String title, String body, String note) {
    this.title = title.strip();
    this.body = body;
    this.note = note == null ? "" : note.strip();
    this.searchRevision++;
  }

  public UUID getId() {
    return id;
  }

  public UUID getOwnerId() {
    return ownerId;
  }

  public String getTitle() {
    return title;
  }

  public String getBody() {
    return body;
  }

  public String getNote() {
    return note;
  }

  public String getProcessingStatus() {
    return processingStatus;
  }

  public String getLifecycleState() {
    return lifecycleState;
  }

  public String getBodyMigrationState() {
    return bodyMigrationState;
  }

  public long getSearchRevision() {
    return searchRevision;
  }

  public long getVersion() {
    return version;
  }
}

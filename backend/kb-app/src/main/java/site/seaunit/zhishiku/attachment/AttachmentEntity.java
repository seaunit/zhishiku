package site.seaunit.zhishiku.attachment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import site.seaunit.zhishiku.persistence.AuditedEntity;
import site.seaunit.zhishiku.richtext.RichImageUploadEntity;

@Entity
@Table(name = "attachment")
public class AttachmentEntity extends AuditedEntity {
  @Id private UUID id;

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(name = "item_id")
  private UUID itemId;

  @Column(nullable = false)
  private String kind;

  @Column(name = "storage_key", nullable = false, unique = true)
  private String storageKey;

  @Column(name = "filename_display", nullable = false)
  private String filenameDisplay;

  @Column(name = "media_type", nullable = false)
  private String mediaType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(nullable = false, length = 64)
  private String sha256;

  @Column(name = "scan_state", nullable = false)
  private String scanState;

  @Column(name = "redaction_state", nullable = false)
  private String redactionState;

  protected AttachmentEntity() {}

  public static AttachmentEntity fromUpload(
      UUID ownerId, UUID itemId, RichImageUploadEntity upload) {
    AttachmentEntity attachment = new AttachmentEntity();
    attachment.id = UUID.randomUUID();
    attachment.ownerId = ownerId;
    attachment.itemId = itemId;
    attachment.kind = "image";
    attachment.storageKey = upload.getStorageKey();
    attachment.filenameDisplay = upload.getFilenameDisplay();
    attachment.mediaType = upload.getMediaType();
    attachment.sizeBytes = upload.getSizeBytes();
    attachment.sha256 = upload.getContentHash();
    attachment.scanState = upload.getScanState();
    attachment.redactionState =
        "none".equals(upload.getRedactionState()) ? "not_required" : upload.getRedactionState();
    return attachment;
  }

  public UUID getId() {
    return id;
  }

  public UUID getOwnerId() {
    return ownerId;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public String getMediaType() {
    return mediaType;
  }

  public String getScanState() {
    return scanState;
  }

  public String getRedactionState() {
    return redactionState;
  }
}

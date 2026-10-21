package site.seaunit.zhishiku.richtext;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import site.seaunit.zhishiku.persistence.AuditedEntity;

@Entity
@Table(name = "rich_image_upload")
public class RichImageUploadEntity extends AuditedEntity {
  @Id private UUID id;

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(name = "item_id")
  private UUID itemId;

  @Column(name = "storage_key", nullable = false, unique = true)
  private String storageKey;

  @Column(name = "filename_display", nullable = false)
  private String filenameDisplay;

  @Column(name = "media_type", nullable = false)
  private String mediaType;

  @Column(name = "source_size_bytes", nullable = false)
  private long sourceSizeBytes;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "content_hash", nullable = false, length = 64)
  private String contentHash;

  @Column(name = "normalized_width", nullable = false)
  private int normalizedWidth;

  @Column(name = "normalized_height", nullable = false)
  private int normalizedHeight;

  @Column(name = "aspect_ratio", nullable = false)
  private double aspectRatio;

  @Column(name = "scan_state", nullable = false)
  private String scanState;

  @Column(name = "redaction_state", nullable = false)
  private String redactionState;

  @Column(nullable = false)
  private String status;

  @Column(name = "error_code")
  private String errorCode;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  protected RichImageUploadEntity() {}

  public static RichImageUploadEntity ready(
      UUID ownerId,
      String storageKey,
      String filename,
      String mediaType,
      long sourceSize,
      long size,
      String hash,
      int width,
      int height) {
    return ready(
        UUID.randomUUID(),
        ownerId,
        storageKey,
        filename,
        mediaType,
        sourceSize,
        size,
        hash,
        width,
        height);
  }

  public static RichImageUploadEntity ready(
      UUID id,
      UUID ownerId,
      String storageKey,
      String filename,
      String mediaType,
      long sourceSize,
      long size,
      String hash,
      int width,
      int height) {
    RichImageUploadEntity upload = new RichImageUploadEntity();
    upload.id = id;
    upload.ownerId = ownerId;
    upload.storageKey = storageKey;
    upload.filenameDisplay = filename;
    upload.mediaType = mediaType;
    upload.sourceSizeBytes = sourceSize;
    upload.sizeBytes = size;
    upload.contentHash = hash;
    upload.normalizedWidth = width;
    upload.normalizedHeight = height;
    upload.aspectRatio = (double) width / height;
    upload.scanState = "clean";
    upload.redactionState = "none";
    upload.status = "ready";
    upload.expiresAt = Instant.now().plusSeconds(24 * 60 * 60);
    return upload;
  }

  public void attach(UUID itemId) {
    if (this.itemId != null && !this.itemId.equals(itemId)) {
      throw new IllegalStateException(
          "Image upload is already attached to another knowledge item.");
    }
    this.itemId = itemId;
    this.status = "attached";
  }

  public void bindItem(UUID itemId) {
    if (itemId == null) {
      return;
    }
    if (this.itemId != null && !this.itemId.equals(itemId)) {
      throw new IllegalStateException("Image upload is already bound to another knowledge item.");
    }
    this.itemId = itemId;
  }

  public void beginScanning() {
    this.status = "scanning";
    this.scanState = "pending";
    this.redactionState = "pending";
  }

  public void reject(String errorCode) {
    this.status = "rejected";
    this.scanState = "rejected";
    this.redactionState = "rejected";
    this.errorCode = errorCode;
  }

  public void expire() {
    this.status = "expired";
    this.expiresAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getOwnerId() {
    return ownerId;
  }

  public UUID getItemId() {
    return itemId;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public String getFilenameDisplay() {
    return filenameDisplay;
  }

  public String getMediaType() {
    return mediaType;
  }

  public long getSourceSizeBytes() {
    return sourceSizeBytes;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public String getContentHash() {
    return contentHash;
  }

  public int getNormalizedWidth() {
    return normalizedWidth;
  }

  public int getNormalizedHeight() {
    return normalizedHeight;
  }

  public double getAspectRatio() {
    return aspectRatio;
  }

  public String getScanState() {
    return scanState;
  }

  public String getRedactionState() {
    return redactionState;
  }

  public String getStatus() {
    return status;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }
}

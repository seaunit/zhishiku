package site.seaunit.zhishiku.richtext;

import com.fasterxml.jackson.databind.JsonNode;
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
@Table(name = "rich_document")
public class RichDocumentEntity extends AuditedEntity {
  @Id
  @Column(name = "item_id")
  private UUID itemId;

  @Column(name = "schema_version", nullable = false)
  private int schemaVersion;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private JsonNode document;

  @Column(name = "plain_text", nullable = false)
  private String plainText;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "document_hash", nullable = false, length = 64)
  private String documentHash;

  @Column(name = "image_count", nullable = false)
  private int imageCount;

  @Column(name = "total_image_bytes", nullable = false)
  private long totalImageBytes;

  @Column(name = "sanitized_at", nullable = false)
  private Instant sanitizedAt;

  @Column(nullable = false)
  private long version;

  protected RichDocumentEntity() {}

  public static RichDocumentEntity create(
      UUID itemId,
      JsonNode document,
      String plainText,
      String hash,
      int imageCount,
      long sizeBytes,
      long version) {
    RichDocumentEntity entity = new RichDocumentEntity();
    entity.itemId = itemId;
    entity.schemaVersion = 1;
    entity.document = document;
    entity.plainText = plainText;
    entity.documentHash = hash;
    entity.imageCount = imageCount;
    entity.totalImageBytes = sizeBytes;
    entity.sanitizedAt = Instant.now();
    entity.version = version;
    return entity;
  }

  public void update(
      JsonNode document,
      String plainText,
      String hash,
      int imageCount,
      long sizeBytes,
      long version) {
    this.document = document;
    this.plainText = plainText;
    this.documentHash = hash;
    this.imageCount = imageCount;
    this.totalImageBytes = sizeBytes;
    this.sanitizedAt = Instant.now();
    this.version = version;
  }

  public UUID getItemId() {
    return itemId;
  }

  public JsonNode getDocument() {
    return document;
  }

  public String getPlainText() {
    return plainText;
  }

  public int getSchemaVersion() {
    return schemaVersion;
  }
}

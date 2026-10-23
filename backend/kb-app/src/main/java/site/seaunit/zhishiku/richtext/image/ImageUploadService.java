package site.seaunit.zhishiku.richtext.image;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import site.seaunit.zhishiku.owner.CurrentOwnerService;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;
import site.seaunit.zhishiku.richtext.RichImageUploadEntity;
import site.seaunit.zhishiku.richtext.RichImageUploadRepository;
import site.seaunit.zhishiku.storage.ObjectStorage;

@Service
public class ImageUploadService {
  private final CurrentOwnerService currentOwnerService;
  private final RichTextItemAccess itemAccess;
  private final ImageInspector inspector;
  private final ImageNormalizer normalizer;
  private final ObjectStorage storage;
  private final RichImageUploadRepository uploads;

  public ImageUploadService(
      CurrentOwnerService currentOwnerService,
      RichTextItemAccess itemAccess,
      ImageInspector inspector,
      ImageNormalizer normalizer,
      ObjectStorage storage,
      RichImageUploadRepository uploads) {
    this.currentOwnerService = currentOwnerService;
    this.itemAccess = itemAccess;
    this.inspector = inspector;
    this.normalizer = normalizer;
    this.storage = storage;
    this.uploads = uploads;
  }

  @Transactional
  public ImageUploadResponse create(
      MultipartFile file, UUID itemId, Authentication authentication) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("An image file is required.");
    }
    if (file.getSize() > ImageInspector.MAX_SOURCE_BYTES) {
      throw new ImageInspectionException("UPLOAD_TOO_LARGE", "Image exceeds 25 MiB.");
    }
    final byte[] sourceBytes;
    try {
      sourceBytes = file.getBytes();
    } catch (IOException error) {
      throw new IllegalStateException("Image could not be read.", error);
    }
    return create(sourceBytes, file.getOriginalFilename(), itemId, authentication);
  }

  @Transactional
  public ImageUploadResponse create(
      byte[] sourceBytes, String originalFilename, UUID itemId, Authentication authentication) {
    OwnerAccountEntity owner = currentOwnerService.require(authentication);
    itemAccess.requireWritableItem(itemId, owner.getId());
    if (sourceBytes == null || sourceBytes.length == 0) {
      throw new IllegalArgumentException("An image file is required.");
    }
    if (sourceBytes.length > ImageInspector.MAX_SOURCE_BYTES) {
      throw new ImageInspectionException("UPLOAD_TOO_LARGE", "Image exceeds 25 MiB.");
    }
    byte[] safeBytes = normalizer.normalize(sourceBytes);
    if (safeBytes == null || safeBytes.length == 0) {
      throw new ImageInspectionException(
          "IMAGE_NORMALIZED_EMPTY", "The image normalizer returned no safe image.");
    }
    ImageInspection inspection = inspector.inspect(safeBytes);
    String hash = sha256(safeBytes);
    Optional<RichImageUploadEntity> duplicate =
        uploads.findFirstByOwnerIdAndContentHashAndStatusAndScanStateOrderByCreatedAtAsc(
            owner.getId(), hash, "ready", "clean");
    if (duplicate.isPresent()
        && java.util.Objects.equals(duplicate.get().getItemId(), itemId)
        && duplicate.get().getExpiresAt().isAfter(java.time.Instant.now())) {
      return response(duplicate.get());
    }
    UUID uploadId = UUID.randomUUID();
    String storageKey =
        "rich-text/" + owner.getId() + "/" + uploadId + "." + inspection.extension();
    final String storedKey;
    try {
      storedKey =
          storage.store(
              storageKey,
              new ByteArrayInputStream(safeBytes),
              safeBytes.length,
              inspection.mediaType());
    } catch (IOException error) {
      storage.delete(storageKey);
      throw new IllegalStateException("Image could not be stored.", error);
    }
    try {
      RichImageUploadEntity upload =
          RichImageUploadEntity.ready(
              uploadId,
              owner.getId(),
              storedKey,
              sanitizeFilename(originalFilename, inspection.extension()),
              inspection.mediaType(),
              sourceBytes.length,
              safeBytes.length,
              hash,
              inspection.width(),
              inspection.height());
      upload.bindItem(itemId);
      upload = uploads.save(upload);
      return response(upload);
    } catch (RuntimeException error) {
      storage.delete(storedKey);
      throw error;
    }
  }

  @Transactional(readOnly = true)
  public ImageUploadResponse get(UUID uploadId, Authentication authentication) {
    OwnerAccountEntity owner = currentOwnerService.require(authentication);
    RichImageUploadEntity upload =
        uploads
            .findByIdAndOwnerId(uploadId, owner.getId())
            .orElseThrow(() -> new IllegalArgumentException("Image upload was not found."));
    return response(upload);
  }

  private static ImageUploadResponse response(RichImageUploadEntity upload) {
    return new ImageUploadResponse(
        upload.getId(),
        upload.getItemId(),
        upload.getStatus(),
        upload.getScanState(),
        upload.getRedactionState(),
        upload.getMediaType(),
        upload.getSourceSizeBytes(),
        upload.getSizeBytes(),
        upload.getContentHash(),
        upload.getNormalizedWidth(),
        upload.getNormalizedHeight(),
        upload.getAspectRatio(),
        upload.getErrorCode(),
        upload.getExpiresAt());
  }

  private static String sanitizeFilename(String original, String extension) {
    String name = original == null ? "image" : original.replaceAll("[\\r\\n\\\\/]", "_").strip();
    if (name.isBlank()) {
      name = "image." + extension;
    }
    return name.length() > 180 ? name.substring(0, 176) + "." + extension : name;
  }

  private static String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 is unavailable.", error);
    }
  }
}

package site.seaunit.zhishiku.richtext.image;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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
  private static final long MAX_SOURCE_BYTES = 25L * 1024 * 1024;

  private final CurrentOwnerService currentOwnerService;
  private final ImageInspector inspector;
  private final ObjectStorage storage;
  private final RichImageUploadRepository uploads;

  public ImageUploadService(
      CurrentOwnerService currentOwnerService,
      ImageInspector inspector,
      ObjectStorage storage,
      RichImageUploadRepository uploads) {
    this.currentOwnerService = currentOwnerService;
    this.inspector = inspector;
    this.storage = storage;
    this.uploads = uploads;
  }

  @Transactional
  public ImageUploadResponse create(
      MultipartFile file, UUID itemId, Authentication authentication) {
    OwnerAccountEntity owner = currentOwnerService.require(authentication);
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("An image file is required.");
    }
    if (file.getSize() > MAX_SOURCE_BYTES) {
      throw new ImageInspectionException("UPLOAD_TOO_LARGE", "Image exceeds 25 MiB.");
    }
    try {
      byte[] bytes = file.getBytes();
      ImageInspection inspection = inspector.inspect(bytes);
      String hash = sha256(bytes);
      UUID uploadId = UUID.randomUUID();
      String storageKey =
          "rich-text/" + owner.getId() + "/" + uploadId + "." + inspection.extension();
      storage.store(
          storageKey, new ByteArrayInputStream(bytes), bytes.length, inspection.mediaType());
      RichImageUploadEntity upload =
          RichImageUploadEntity.ready(
              owner.getId(),
              storageKey,
              sanitizeFilename(file.getOriginalFilename(), inspection.extension()),
              inspection.mediaType(),
              bytes.length,
              bytes.length,
              hash,
              inspection.width(),
              inspection.height());
      upload = uploads.save(upload);
      return response(upload);
    } catch (IOException error) {
      throw new IllegalStateException("Image could not be stored.", error);
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

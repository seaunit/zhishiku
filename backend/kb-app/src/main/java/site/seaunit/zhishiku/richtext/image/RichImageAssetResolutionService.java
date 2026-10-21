package site.seaunit.zhishiku.richtext.image;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.seaunit.zhishiku.attachment.AttachmentEntity;
import site.seaunit.zhishiku.attachment.AttachmentRepository;
import site.seaunit.zhishiku.richtext.RichImageUploadEntity;
import site.seaunit.zhishiku.richtext.RichImageUploadRepository;

@Service
public class RichImageAssetResolutionService {
  private final AttachmentRepository attachments;
  private final RichImageUploadRepository uploads;

  public RichImageAssetResolutionService(
      AttachmentRepository attachments, RichImageUploadRepository uploads) {
    this.attachments = attachments;
    this.uploads = uploads;
  }

  @Transactional(readOnly = true)
  public RichImageAssetSelection resolveForBilling(
      Collection<UUID> requestedAssetIds, UUID ownerId) {
    if (requestedAssetIds == null || requestedAssetIds.isEmpty()) {
      return new RichImageAssetSelection(List.of(), List.of());
    }
    if (requestedAssetIds.size() > 100) {
      throw new IllegalArgumentException("Too many image assets were requested.");
    }
    List<UUID> ids = requestedAssetIds.stream().distinct().toList();
    Map<UUID, AttachmentEntity> attachmentById =
        attachments.findByOwnerIdAndIdIn(ownerId, ids).stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    AttachmentEntity::getId, attachment -> attachment, (first, ignored) -> first));
    Map<UUID, RichImageUploadEntity> uploadById =
        uploads.findByOwnerIdAndIdIn(ownerId, ids).stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    RichImageUploadEntity::getId, upload -> upload, (first, ignored) -> first));
    Map<String, UUID> uniqueByHash = new LinkedHashMap<>();
    for (UUID assetId : ids) {
      ResolvedRichImage resolved =
          resolve(assetId, attachmentById.get(assetId), uploadById.get(assetId));
      uniqueByHash.putIfAbsent(resolved.contentHash(), resolved.assetId());
    }
    return new RichImageAssetSelection(
        List.copyOf(uniqueByHash.values()), List.copyOf(uniqueByHash.keySet()));
  }

  @Transactional
  public ResolvedRichImage promoteForDocument(UUID assetId, UUID ownerId, UUID itemId) {
    AttachmentEntity existingAttachment =
        attachments.findByIdAndOwnerId(assetId, ownerId).orElse(null);
    if (existingAttachment != null) {
      requireSafeAttachment(existingAttachment);
      return resolvedAttachment(existingAttachment, assetId);
    }
    RichImageUploadEntity upload =
        uploads
            .findByIdAndOwnerId(assetId, ownerId)
            .orElseThrow(() -> new IllegalArgumentException("Image asset was not found."));
    requireSafeUpload(upload);
    if (upload.getItemId() != null && !upload.getItemId().equals(itemId)) {
      throw new IllegalArgumentException("Image upload belongs to another knowledge item.");
    }
    AttachmentEntity attachment =
        attachments
            .findFirstByOwnerIdAndSha256AndScanStateOrderByCreatedAtAsc(
                ownerId, upload.getContentHash(), "clean")
            .filter(this::isSafeAttachment)
            .orElseGet(
                () -> attachments.save(AttachmentEntity.fromUpload(ownerId, itemId, upload)));
    upload.attach(itemId);
    uploads.save(upload);
    return new ResolvedRichImage(
        attachment.getId(),
        attachment.getSha256(),
        upload.getSizeBytes(),
        upload.getMediaType(),
        upload.getNormalizedWidth(),
        upload.getNormalizedHeight());
  }

  private ResolvedRichImage resolve(
      UUID requestedId, AttachmentEntity attachment, RichImageUploadEntity upload) {
    if (attachment != null) {
      requireSafeAttachment(attachment);
      return resolvedAttachment(attachment, requestedId);
    }
    if (upload != null) {
      requireSafeUpload(upload);
      return new ResolvedRichImage(
          requestedId,
          upload.getContentHash(),
          upload.getSizeBytes(),
          upload.getMediaType(),
          upload.getNormalizedWidth(),
          upload.getNormalizedHeight());
    }
    throw new IllegalArgumentException("Image asset was not found for the current owner.");
  }

  private void requireSafeAttachment(AttachmentEntity attachment) {
    if (!isSafeAttachment(attachment)) {
      throw new IllegalArgumentException("Image asset is not safe to use.");
    }
  }

  private boolean isSafeAttachment(AttachmentEntity attachment) {
    return "clean".equals(attachment.getScanState())
        && List.of("none", "not_required", "redacted").contains(attachment.getRedactionState());
  }

  private static ResolvedRichImage resolvedAttachment(
      AttachmentEntity attachment, UUID requestedId) {
    return new ResolvedRichImage(
        requestedId,
        attachment.getSha256(),
        attachment.getSizeBytes(),
        attachment.getMediaType(),
        0,
        0);
  }

  private static void requireSafeUpload(RichImageUploadEntity upload) {
    if (!"ready".equals(upload.getStatus())
        || !"clean".equals(upload.getScanState())
        || !List.of("none", "redacted").contains(upload.getRedactionState())) {
      throw new IllegalArgumentException("Image upload is not safe to use.");
    }
  }
}

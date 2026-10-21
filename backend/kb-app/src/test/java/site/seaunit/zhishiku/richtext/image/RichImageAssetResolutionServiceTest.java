package site.seaunit.zhishiku.richtext.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import site.seaunit.zhishiku.attachment.AttachmentEntity;
import site.seaunit.zhishiku.attachment.AttachmentRepository;
import site.seaunit.zhishiku.richtext.RichImageUploadEntity;
import site.seaunit.zhishiku.richtext.RichImageUploadRepository;

class RichImageAssetResolutionServiceTest {
  private final AttachmentRepository attachments = mock(AttachmentRepository.class);
  private final RichImageUploadRepository uploads = mock(RichImageUploadRepository.class);
  private final RichImageAssetResolutionService service =
      new RichImageAssetResolutionService(attachments, uploads);
  private final UUID ownerId = UUID.randomUUID();

  @Test
  void imageAssetIdsAreDeduplicatedByContentHashAndKeepRequestOrder() {
    UUID firstId = UUID.randomUUID();
    UUID duplicateId = UUID.randomUUID();
    UUID thirdId = UUID.randomUUID();
    RichImageUploadEntity first = upload(firstId, ownerId, "a".repeat(64), "ready", "clean");
    RichImageUploadEntity duplicate =
        upload(duplicateId, ownerId, "a".repeat(64), "ready", "clean");
    RichImageUploadEntity third = upload(thirdId, ownerId, "b".repeat(64), "ready", "clean");
    when(uploads.findByOwnerIdAndIdIn(eq(ownerId), any()))
        .thenReturn(List.of(first, duplicate, third));
    when(attachments.findByOwnerIdAndIdIn(eq(ownerId), any())).thenReturn(List.of());

    RichImageAssetSelection selection =
        service.resolveForBilling(List.of(firstId, duplicateId, thirdId), ownerId);

    assertThat(selection.imageAssetIds()).containsExactly(firstId, thirdId);
    assertThat(selection.contentHashes()).containsExactly("a".repeat(64), "b".repeat(64));
  }

  @Test
  void ownerIsolationDoesNotResolveAnotherOwnersUpload() {
    UUID uploadId = UUID.randomUUID();
    when(uploads.findByOwnerIdAndIdIn(eq(ownerId), any())).thenReturn(List.of());
    when(attachments.findByOwnerIdAndIdIn(eq(ownerId), any())).thenReturn(List.of());

    assertThatThrownBy(() -> service.resolveForBilling(List.of(uploadId), ownerId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not found");
  }

  @Test
  void rejectsUnscannedOrRejectedAssets() {
    UUID uploadId = UUID.randomUUID();
    RichImageUploadEntity pending =
        upload(uploadId, ownerId, "c".repeat(64), "scanning", "pending");
    when(uploads.findByOwnerIdAndIdIn(eq(ownerId), any())).thenReturn(List.of(pending));
    when(attachments.findByOwnerIdAndIdIn(eq(ownerId), any())).thenReturn(List.of());

    assertThatThrownBy(() -> service.resolveForBilling(List.of(uploadId), ownerId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("safe");
  }

  @Test
  void promotionReusesAnExistingSafeAttachmentWithTheSameHash() {
    UUID uploadId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    RichImageUploadEntity upload = upload(uploadId, ownerId, "d".repeat(64), "ready", "clean");
    AttachmentEntity existing = AttachmentEntity.fromUpload(ownerId, itemId, upload);
    when(attachments.findByIdAndOwnerId(uploadId, ownerId)).thenReturn(Optional.empty());
    when(uploads.findByIdAndOwnerId(uploadId, ownerId)).thenReturn(Optional.of(upload));
    when(attachments.findFirstByOwnerIdAndSha256AndScanStateOrderByCreatedAtAsc(
            ownerId, upload.getContentHash(), "clean"))
        .thenReturn(Optional.of(existing));

    ResolvedRichImage resolved = service.promoteForDocument(uploadId, ownerId, itemId);

    assertThat(resolved.assetId()).isEqualTo(existing.getId());
    verify(attachments, never()).save(any());
    verify(uploads).save(upload);
    assertThat(upload.getStatus()).isEqualTo("attached");
  }

  private static RichImageUploadEntity upload(
      UUID id, UUID ownerId, String hash, String status, String scanState) {
    RichImageUploadEntity upload =
        RichImageUploadEntity.ready(
            id, ownerId, "rich-text/" + id + ".png", "image.png", "image/png", 10, 10, hash, 1, 1);
    if (!"ready".equals(status)) {
      upload.beginScanning();
    }
    return upload;
  }
}

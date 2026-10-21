package site.seaunit.zhishiku.richtext.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import site.seaunit.zhishiku.owner.CurrentOwnerService;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;
import site.seaunit.zhishiku.richtext.RichImageUploadEntity;
import site.seaunit.zhishiku.richtext.RichImageUploadRepository;
import site.seaunit.zhishiku.storage.ObjectStorage;

class ImageUploadServiceTest {
  private static final byte[] NORMALIZED = "safe-normalized-image".getBytes();

  private final CurrentOwnerService owners = mock(CurrentOwnerService.class);
  private final RichTextItemAccess itemAccess = mock(RichTextItemAccess.class);
  private final ImageInspector inspector = mock(ImageInspector.class);
  private final ImageNormalizer normalizer = mock(ImageNormalizer.class);
  private final ObjectStorage storage = mock(ObjectStorage.class);
  private final RichImageUploadRepository uploads = mock(RichImageUploadRepository.class);
  private final Authentication authentication = mock(Authentication.class);
  private final OwnerAccountEntity owner =
      OwnerAccountEntity.create("owner@example.com", "hash", "zh-CN");
  private final ImageUploadService service =
      new ImageUploadService(owners, itemAccess, inspector, normalizer, storage, uploads);

  @BeforeEach
  void setUp() throws Exception {
    when(owners.require(authentication)).thenReturn(owner);
    when(normalizer.normalize(any(byte[].class))).thenReturn(NORMALIZED);
    when(inspector.inspect(NORMALIZED)).thenReturn(new ImageInspection("image/png", 1, 1, "png"));
    when(storage.store(anyString(), any(ByteArrayInputStream.class), anyLong(), eq("image/png")))
        .thenReturn("stored");
    when(uploads.save(any(RichImageUploadEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(uploads.findFirstByOwnerIdAndContentHashAndStatusAndScanStateOrderByCreatedAtAsc(
            any(), anyString(), eq("ready"), eq("clean")))
        .thenReturn(Optional.empty());
  }

  @Test
  void rejectsFilesAboveTwentyFiveMebibytesBeforeReadingThem() {
    MultipartFile file = file(25L * 1024 * 1024 + 1);

    assertThatThrownBy(() -> service.create(file, null, authentication))
        .isInstanceOf(ImageInspectionException.class)
        .extracting(error -> ((ImageInspectionException) error).getCode())
        .isEqualTo("UPLOAD_TOO_LARGE");
    verify(normalizer, never()).normalize(any());
  }

  @Test
  void verifiesItemOwnershipBeforeStoringTheUpload() throws Exception {
    doThrow(new IllegalArgumentException("Knowledge item was not found."))
        .when(itemAccess)
        .requireWritableItem(any(), eq(owner.getId()));

    assertThatThrownBy(() -> service.create(file(10), UUID.randomUUID(), authentication))
        .isInstanceOf(IllegalArgumentException.class);
    verify(normalizer, never()).normalize(any());
    verify(storage, never()).store(anyString(), any(), anyLong(), anyString());
  }

  @Test
  void persistsOnlyNormalizedBytesAndTheirHash() throws Exception {
    MultipartFile file = file(100);

    ImageUploadResponse response = service.create(file, null, authentication);

    ArgumentCaptor<RichImageUploadEntity> captured =
        ArgumentCaptor.forClass(RichImageUploadEntity.class);
    verify(uploads).save(captured.capture());
    RichImageUploadEntity upload = captured.getValue();
    assertThat(upload.getSourceSizeBytes()).isEqualTo(100);
    assertThat(upload.getSizeBytes()).isEqualTo(NORMALIZED.length);
    assertThat(upload.getContentHash()).isEqualTo(sha256(NORMALIZED));
    assertThat(response.contentHash()).isEqualTo(sha256(NORMALIZED));
  }

  @Test
  void bindsANewUploadToTheAuthorizedItemWithoutMarkingItAttached() {
    UUID itemId = UUID.randomUUID();

    service.create(file(10), itemId, authentication);

    ArgumentCaptor<RichImageUploadEntity> captured =
        ArgumentCaptor.forClass(RichImageUploadEntity.class);
    verify(uploads).save(captured.capture());
    assertThat(captured.getValue().getItemId()).isEqualTo(itemId);
    assertThat(captured.getValue().getStatus()).isEqualTo("ready");
  }

  @Test
  void doesNotReuseAReadyUploadBoundToAnotherItem() {
    UUID itemId = UUID.randomUUID();
    RichImageUploadEntity existing = upload(sha256(NORMALIZED));
    existing.bindItem(UUID.randomUUID());
    when(uploads.findFirstByOwnerIdAndContentHashAndStatusAndScanStateOrderByCreatedAtAsc(
            owner.getId(), sha256(NORMALIZED), "ready", "clean"))
        .thenReturn(Optional.of(existing));

    ImageUploadResponse response = service.create(file(10), itemId, authentication);

    assertThat(response.uploadId()).isNotEqualTo(existing.getId());
    verify(uploads).save(any(RichImageUploadEntity.class));
  }

  @Test
  void doesNotReuseAnExpiredReadyUpload() {
    RichImageUploadEntity existing = upload(sha256(NORMALIZED));
    existing.expire();
    when(uploads.findFirstByOwnerIdAndContentHashAndStatusAndScanStateOrderByCreatedAtAsc(
            owner.getId(), sha256(NORMALIZED), "ready", "clean"))
        .thenReturn(Optional.of(existing));

    ImageUploadResponse response = service.create(file(10), null, authentication);

    assertThat(response.uploadId()).isNotEqualTo(existing.getId());
    verify(uploads).save(any(RichImageUploadEntity.class));
  }

  @Test
  void reusesReadyUploadWithTheSameOwnerAndContentHash() throws Exception {
    String hash = sha256(NORMALIZED);
    RichImageUploadEntity existing = upload(hash);
    when(uploads.findFirstByOwnerIdAndContentHashAndStatusAndScanStateOrderByCreatedAtAsc(
            owner.getId(), hash, "ready", "clean"))
        .thenReturn(Optional.of(existing));

    ImageUploadResponse response = service.create(file(10), null, authentication);

    assertThat(response.uploadId()).isEqualTo(existing.getId());
    verify(storage, never()).store(anyString(), any(), anyLong(), anyString());
    verify(uploads, never()).save(any());
  }

  @Test
  void scanFailureLeavesNoStagedOrStoredObject() throws Exception {
    when(inspector.inspect(NORMALIZED))
        .thenThrow(new ImageInspectionException("IMAGE_SECRET_DETECTED", "secret"));

    assertThatThrownBy(() -> service.create(file(10), null, authentication))
        .isInstanceOf(ImageInspectionException.class);
    verify(storage, never()).store(anyString(), any(), anyLong(), anyString());
    verify(uploads, never()).save(any());
  }

  @Test
  void persistenceFailureDeletesTheNewlyStoredObject() throws Exception {
    when(storage.store(anyString(), any(ByteArrayInputStream.class), anyLong(), eq("image/png")))
        .thenReturn("stored-key");
    when(uploads.save(any(RichImageUploadEntity.class)))
        .thenThrow(new IllegalStateException("database unavailable"));

    assertThatThrownBy(() -> service.create(file(10), null, authentication))
        .isInstanceOf(IllegalStateException.class);
    verify(storage).delete("stored-key");
  }

  @Test
  void storageFailureAttemptsCleanupAndNeverPersistsAnUpload() throws Exception {
    when(storage.store(anyString(), any(ByteArrayInputStream.class), anyLong(), eq("image/png")))
        .thenThrow(new IOException("storage unavailable"));

    assertThatThrownBy(() -> service.create(file(10), null, authentication))
        .isInstanceOf(IllegalStateException.class);
    verify(storage).delete(anyString());
    verify(uploads, never()).save(any());
  }

  private static MultipartFile file(long size) {
    MultipartFile file = mock(MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(size);
    when(file.getOriginalFilename()).thenReturn("safe.png");
    try {
      when(file.getBytes()).thenReturn(new byte[(int) size]);
    } catch (IOException error) {
      throw new AssertionError(error);
    }
    return file;
  }

  private static RichImageUploadEntity upload(String hash) {
    RichImageUploadEntity upload =
        RichImageUploadEntity.ready(
            UUID.randomUUID(),
            "rich-text/existing.png",
            "existing.png",
            "image/png",
            10,
            NORMALIZED.length,
            hash,
            1,
            1);
    return upload;
  }

  private static String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (Exception error) {
      throw new AssertionError(error);
    }
  }
}

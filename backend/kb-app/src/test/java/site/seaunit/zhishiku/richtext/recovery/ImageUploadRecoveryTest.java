package site.seaunit.zhishiku.richtext.recovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import site.seaunit.zhishiku.owner.CurrentOwnerService;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;
import site.seaunit.zhishiku.richtext.RichImageUploadEntity;
import site.seaunit.zhishiku.richtext.RichImageUploadRepository;
import site.seaunit.zhishiku.richtext.image.ImageInspection;
import site.seaunit.zhishiku.richtext.image.ImageInspector;
import site.seaunit.zhishiku.richtext.image.ImageNormalizer;
import site.seaunit.zhishiku.richtext.image.ImageUploadResponse;
import site.seaunit.zhishiku.richtext.image.ImageUploadService;
import site.seaunit.zhishiku.richtext.image.RichTextItemAccess;
import site.seaunit.zhishiku.storage.ObjectStorage;

class ImageUploadRecoveryTest {
  private static final byte[] SAFE_IMAGE = "safe-image".getBytes();

  private final Authentication authentication = mock(Authentication.class);
  private final OwnerAccountEntity owner =
      OwnerAccountEntity.create("owner@example.com", "hash", "zh-CN");
  private final RichImageUploadRepository uploads = mock(RichImageUploadRepository.class);
  private final Map<UUID, RichImageUploadEntity> durableUploads = new HashMap<>();

  @Test
  void readyUploadAndItemBindingSurviveServiceRecreation() throws Exception {
    ImageUploadService firstProcess = service();
    UUID itemId = UUID.randomUUID();

    ImageUploadResponse uploaded =
        firstProcess.create(
            new MockMultipartFile("file", "diagram.png", "image/png", SAFE_IMAGE),
            itemId,
            authentication);

    ImageUploadService restartedProcess = service();
    ImageUploadResponse recovered = restartedProcess.get(uploaded.uploadId(), authentication);

    assertThat(recovered.uploadId()).isEqualTo(uploaded.uploadId());
    assertThat(recovered.itemId()).isEqualTo(itemId);
    assertThat(recovered.status()).isEqualTo("ready");
    assertThat(recovered.contentHash()).isEqualTo(uploaded.contentHash());
  }

  @Test
  void expiredUploadIsNotRevivedByAProcessRestart() {
    RichImageUploadEntity expired =
        RichImageUploadEntity.ready(
            owner.getId(),
            "rich-text/expired.png",
            "expired.png",
            "image/png",
            SAFE_IMAGE.length,
            SAFE_IMAGE.length,
            "a".repeat(64),
            1,
            1);
    expired.expire();
    durableUploads.put(expired.getId(), expired);

    ImageUploadResponse recovered = service().get(expired.getId(), authentication);

    assertThat(recovered.status()).isEqualTo("expired");
    assertThat(recovered.expiresAt()).isBeforeOrEqualTo(java.time.Instant.now());
  }

  private ImageUploadService service() {
    CurrentOwnerService owners = mock(CurrentOwnerService.class);
    when(owners.require(authentication)).thenReturn(owner);
    RichTextItemAccess itemAccess = mock(RichTextItemAccess.class);
    ImageInspector inspector = mock(ImageInspector.class);
    when(inspector.inspect(any(byte[].class)))
        .thenReturn(new ImageInspection("image/png", 1, 1, "png"));
    ImageNormalizer normalizer = mock(ImageNormalizer.class);
    when(normalizer.normalize(any(byte[].class))).thenReturn(SAFE_IMAGE);
    when(uploads.findFirstByOwnerIdAndContentHashAndStatusAndScanStateOrderByCreatedAtAsc(
            any(), anyString(), eq("ready"), eq("clean")))
        .thenReturn(Optional.empty());
    when(uploads.save(any(RichImageUploadEntity.class)))
        .thenAnswer(
            invocation -> {
              RichImageUploadEntity saved = invocation.getArgument(0);
              durableUploads.put(saved.getId(), saved);
              return saved;
            });
    when(uploads.findByIdAndOwnerId(any(), eq(owner.getId())))
        .thenAnswer(
            invocation ->
                Optional.ofNullable(durableUploads.get(invocation.getArgument(0, UUID.class))));
    ObjectStorage storage = mock(ObjectStorage.class);
    try {
      when(storage.store(anyString(), any(ByteArrayInputStream.class), anyLong(), eq("image/png")))
          .thenReturn("stored");
    } catch (java.io.IOException error) {
      throw new AssertionError(error);
    }
    return new ImageUploadService(owners, itemAccess, inspector, normalizer, storage, uploads);
  }
}

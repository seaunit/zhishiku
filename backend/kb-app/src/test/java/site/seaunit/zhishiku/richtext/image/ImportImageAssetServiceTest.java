package site.seaunit.zhishiku.richtext.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import site.seaunit.zhishiku.ocr.TesseractOcrService;
import site.seaunit.zhishiku.webimport.AiTextSecretRedactor;

class ImportImageAssetServiceTest {
  private static final byte[] PNG = new byte[] {(byte) 0x89, 'P', 'N', 'G'};

  private final ImageUploadService uploads = mock(ImageUploadService.class);
  private final ImageInspector inspector = mock(ImageInspector.class);
  private final ImageNormalizer normalizer = source -> source;
  private final TesseractOcrService ocr = mock(TesseractOcrService.class);
  private final Authentication authentication = mock(Authentication.class);
  private final ImportImageAssetService service =
      new ImportImageAssetService(uploads, inspector, normalizer, ocr, new AiTextSecretRedactor());

  @Test
  void uploadsAlreadyTextCheckedSourceImages() {
    UUID uploadId = UUID.randomUUID();
    when(inspector.inspect(PNG)).thenReturn(new ImageInspection("image/png", 1200, 800, "png"));
    when(uploads.create(eq(PNG), eq("diagram.png"), eq(null), eq(authentication)))
        .thenReturn(
            new ImageUploadResponse(
                uploadId,
                null,
                "ready",
                "clean",
                "none",
                "image/png",
                PNG.length,
                PNG.length,
                "a".repeat(64),
                1200,
                800,
                1.5,
                null,
                Instant.now().plusSeconds(3600)));

    List<ImportedImageAsset> result =
        service.prepare(
            List.of(new ImportImageCandidate(PNG, "diagram.png", "image/png")),
            authentication,
            "架构说明",
            "来源图示",
            true);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).uploadId()).isEqualTo(uploadId);
    assertThat(result.get(0).alt()).isEqualTo("架构说明 图示");
    verify(ocr, never()).recognize(any());
  }

  @Test
  void skipsEmbeddedImagesWhoseRecognizedTextContainsSecrets() {
    when(inspector.inspect(PNG)).thenReturn(new ImageInspection("image/png", 1200, 800, "png"));
    when(ocr.recognize(any())).thenReturn("password=super-secret-value");

    List<ImportedImageAsset> result =
        service.prepare(
            List.of(new ImportImageCandidate(PNG, "diagram.png", "image/png")),
            authentication,
            "架构说明",
            "来源图示",
            false);

    assertThat(result).isEmpty();
    verify(uploads, never()).create(any(), any(), any(), any());
  }

  @Test
  void skipsSmallDecorativeImagesInsteadOfCallingThemSourceFigures() {
    when(inspector.inspect(PNG)).thenReturn(new ImageInspection("image/png", 80, 80, "png"));

    List<ImportedImageAsset> result =
        service.prepare(
            List.of(new ImportImageCandidate(PNG, "icon.png", "image/png")),
            authentication,
            "架构说明",
            "来源图示",
            true);

    assertThat(result).isEmpty();
    verify(uploads, never()).create(any(), any(), any(), any());
  }
}

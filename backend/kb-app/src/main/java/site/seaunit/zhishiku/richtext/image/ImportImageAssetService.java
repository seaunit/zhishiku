package site.seaunit.zhishiku.richtext.image;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import site.seaunit.zhishiku.ocr.TesseractOcrService;
import site.seaunit.zhishiku.webimport.AiTextSecretRedactor;

@Service
public class ImportImageAssetService {
  private static final int MAX_IMAGES = 6;
  private static final Set<String> NORMALIZE_FIRST_TYPES = Set.of("image/heic", "image/heif");
  private static final int MIN_SOURCE_FIGURE_EDGE = 120;
  private static final long MIN_SOURCE_FIGURE_PIXELS = 40_000L;
  private static final int MAX_ALT_CHARS = 500;
  private static final int MAX_CAPTION_CHARS = 500;

  private final ImageUploadService uploads;
  private final ImageInspector inspector;
  private final ImageNormalizer normalizer;
  private final TesseractOcrService ocr;
  private final AiTextSecretRedactor secretRedactor;

  public ImportImageAssetService(
      ImageUploadService uploads,
      ImageInspector inspector,
      ImageNormalizer normalizer,
      TesseractOcrService ocr,
      AiTextSecretRedactor secretRedactor) {
    this.uploads = uploads;
    this.inspector = inspector;
    this.normalizer = normalizer;
    this.ocr = ocr;
    this.secretRedactor = secretRedactor;
  }

  public List<ImportedImageAsset> prepare(
      List<ImportImageCandidate> candidates,
      Authentication authentication,
      String title,
      String caption,
      boolean sourceTextSafetyChecked) {
    if (candidates == null || candidates.isEmpty()) {
      return List.of();
    }
    List<ImportedImageAsset> prepared = new ArrayList<>();
    for (ImportImageCandidate candidate : candidates) {
      if (prepared.size() >= MAX_IMAGES) {
        break;
      }
      try {
        byte[] safeBytes = normalizeIfNeeded(candidate);
        ImageInspection inspection = inspector.inspect(safeBytes);
        if (inspection.width() < MIN_SOURCE_FIGURE_EDGE
            || inspection.height() < MIN_SOURCE_FIGURE_EDGE
            || (long) inspection.width() * inspection.height() < MIN_SOURCE_FIGURE_PIXELS) {
          continue;
        }
        if (!sourceTextSafetyChecked && containsSecretInImage(safeBytes)) {
          continue;
        }
        ImageUploadResponse upload =
            uploads.create(safeBytes, candidate.filename(), null, authentication);
        prepared.add(
            new ImportedImageAsset(
                upload.uploadId(),
                limit(title == null || title.isBlank() ? "来源图示" : title + " 图示", MAX_ALT_CHARS),
                limit(caption == null || caption.isBlank() ? "来源图示" : caption, MAX_CAPTION_CHARS),
                upload.normalizedWidth(),
                upload.normalizedHeight(),
                upload.aspectRatio()));
      } catch (RuntimeException ignored) {
        // A non-critical source image must not fail text knowledge that is otherwise usable.
      }
    }
    return List.copyOf(prepared);
  }

  private byte[] normalizeIfNeeded(ImportImageCandidate candidate) {
    String mediaType = candidate.mediaType().toLowerCase(java.util.Locale.ROOT);
    return NORMALIZE_FIRST_TYPES.contains(mediaType)
        ? normalizer.normalize(candidate.bytes())
        : candidate.bytes();
  }

  private boolean containsSecretInImage(byte[] bytes) {
    Path image = null;
    try {
      image = Files.createTempFile("zhishiku-import-image-scan-", ".img");
      Files.write(image, bytes);
      return secretRedactor.redact(ocr.recognize(image)).applied();
    } catch (IOException | RuntimeException error) {
      // If image text cannot be checked, skip the image and preserve text knowledge.
      return true;
    } finally {
      if (image != null) {
        try {
          Files.deleteIfExists(image);
        } catch (IOException ignored) {
          image.toFile().deleteOnExit();
        }
      }
    }
  }

  private static String limit(String value, int maxLength) {
    return value.substring(0, Math.min(value.length(), maxLength));
  }
}

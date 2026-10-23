package site.seaunit.zhishiku.screenshotimport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import site.seaunit.zhishiku.ocr.TesseractOcrService;
import site.seaunit.zhishiku.richtext.image.ImageInspection;
import site.seaunit.zhishiku.richtext.image.ImageInspectionException;
import site.seaunit.zhishiku.richtext.image.ImageInspector;
import site.seaunit.zhishiku.richtext.image.ImageNormalizer;
import site.seaunit.zhishiku.richtext.image.ImportImageCandidate;
import site.seaunit.zhishiku.webimport.WebImportUnavailableException;

@Component
public class ScreenshotTextExtractor {
  private static final int MIN_TEXT_CHARS = 30;
  private static final Set<String> OCR_TYPES = Set.of("image/png", "image/jpeg");

  private final ImageInspector imageInspector;
  private final ImageNormalizer imageNormalizer;
  private final TesseractOcrService ocr;
  private final int maxTextChars;

  public ScreenshotTextExtractor(
      ImageInspector imageInspector,
      ImageNormalizer imageNormalizer,
      TesseractOcrService ocr,
      @Value("${app.screenshot-import.max-text-chars:30000}") int maxTextChars) {
    this.imageInspector = imageInspector;
    this.imageNormalizer = imageNormalizer;
    this.ocr = ocr;
    this.maxTextChars = Math.max(1_000, maxTextChars);
  }

  public ExtractedScreenshot extract(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw failure(
          "SCREENSHOT_EMPTY", HttpStatus.UNPROCESSABLE_ENTITY, "The screenshot is empty.");
    }
    if (file.getSize() > ImageInspector.MAX_SOURCE_BYTES) {
      throw failure(
          "SCREENSHOT_TOO_LARGE",
          HttpStatus.PAYLOAD_TOO_LARGE,
          "The screenshot exceeds the 25 MiB limit.");
    }
    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (IOException error) {
      throw failure(
          "SCREENSHOT_IMPORT_UNAVAILABLE",
          HttpStatus.SERVICE_UNAVAILABLE,
          "The screenshot could not be read.");
    }

    ImageInspection inspection;
    try {
      inspection = imageInspector.inspect(bytes);
    } catch (ImageInspectionException error) {
      throw failure(
          "SCREENSHOT_IMAGE_INVALID",
          HttpStatus.UNPROCESSABLE_ENTITY,
          "The screenshot image was rejected by the safety checks.");
    }
    if (!OCR_TYPES.contains(inspection.mediaType())) {
      throw failure(
          "SCREENSHOT_UNSUPPORTED",
          HttpStatus.UNSUPPORTED_MEDIA_TYPE,
          "Only PNG and JPEG screenshots can be recognized.");
    }
    if ("image/png".equals(inspection.mediaType()) && containsAscii(bytes, "acTL")) {
      throw failure(
          "SCREENSHOT_IMAGE_INVALID",
          HttpStatus.UNPROCESSABLE_ENTITY,
          "Animated PNG screenshots are not accepted.");
    }
    byte[] normalized;
    try {
      normalized = imageNormalizer.normalize(bytes);
    } catch (ImageInspectionException error) {
      throw failure(
          "SCREENSHOT_IMPORT_UNAVAILABLE",
          HttpStatus.SERVICE_UNAVAILABLE,
          "Screenshot normalization is unavailable.");
    }

    Path screenshot = null;
    try {
      screenshot = Files.createTempFile("zhishiku-screenshot-", ".png");
      Files.write(screenshot, normalized);
      String text = ocr.recognize(screenshot);
      if (text.length() > maxTextChars) {
        throw failure(
            "SCREENSHOT_TOO_LONG",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "The recognized screenshot text exceeds the configured limit.");
      }
      if (text.codePoints().filter(Character::isLetterOrDigit).count() < MIN_TEXT_CHARS) {
        throw failure(
            "SCREENSHOT_NO_TEXT",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "The screenshot did not contain enough readable text.");
      }
      String format = "image/png".equals(inspection.mediaType()) ? "PNG 截图" : "JPEG 截图";
      return new ExtractedScreenshot(
          text,
          inspection.mediaType(),
          format,
          text.length(),
          List.of(
              new ImportImageCandidate(
                  normalized,
                  safeFilename(file.getOriginalFilename(), inspection.extension()),
                  inspection.mediaType())));
    } catch (WebImportUnavailableException error) {
      throw failure(
          "SCREENSHOT_IMPORT_UNAVAILABLE",
          HttpStatus.SERVICE_UNAVAILABLE,
          "Screenshot text recognition is unavailable.");
    } catch (IOException error) {
      throw failure(
          "SCREENSHOT_IMPORT_UNAVAILABLE",
          HttpStatus.SERVICE_UNAVAILABLE,
          "Screenshot processing is unavailable.");
    } finally {
      if (screenshot != null) {
        try {
          Files.deleteIfExists(screenshot);
        } catch (IOException ignored) {
          screenshot.toFile().deleteOnExit();
        }
      }
    }
  }

  private static ScreenshotImportException failure(String code, HttpStatus status, String message) {
    return new ScreenshotImportException(code, status, message);
  }

  private static String safeFilename(String value, String extension) {
    String name = value == null || value.isBlank() ? "clipboard-screenshot" : value;
    name = name.replaceAll("[\\r\\n\\\\/]", "_").strip();
    return name.toLowerCase(java.util.Locale.ROOT).endsWith("." + extension)
        ? name
        : name + "." + extension;
  }

  private static boolean containsAscii(byte[] bytes, String value) {
    byte[] target = value.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    outer:
    for (int index = 0; index <= bytes.length - target.length; index++) {
      for (int offset = 0; offset < target.length; offset++) {
        if (bytes[index + offset] != target[offset]) {
          continue outer;
        }
      }
      return true;
    }
    return false;
  }
}

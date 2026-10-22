package site.seaunit.zhishiku.screenshotimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import site.seaunit.zhishiku.ocr.TesseractOcrService;
import site.seaunit.zhishiku.richtext.image.ImageInspector;
import site.seaunit.zhishiku.richtext.image.ImageNormalizer;
import site.seaunit.zhishiku.webimport.WebImportUnavailableException;

class ScreenshotTextExtractorTest {
  private static final byte[] PNG =
      Base64.getDecoder()
          .decode(
              "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl2nGQAAAAASUVORK5CYII=");

  private final TesseractOcrService ocr = mock(TesseractOcrService.class);
  private final ImageNormalizer normalizer = source -> source;
  private final ScreenshotTextExtractor extractor =
      new ScreenshotTextExtractor(new ImageInspector(), normalizer, ocr, 30_000);

  @Test
  void recognizesPngScreenshotTextWithoutPersistingTheImage() {
    when(ocr.recognize(any())).thenReturn("截图中的完整可用正文".repeat(10));
    MockMultipartFile file = new MockMultipartFile("file", "clipboard.png", "image/png", PNG);

    ExtractedScreenshot screenshot = extractor.extract(file);

    assertThat(screenshot.mediaType()).isEqualTo("image/png");
    assertThat(screenshot.formatLabel()).isEqualTo("PNG 截图");
    assertThat(screenshot.text()).contains("截图中的完整可用正文");
  }

  @Test
  void rejectsEmptyScreenshot() {
    MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

    assertThatThrownBy(() -> extractor.extract(file))
        .isInstanceOf(ScreenshotImportException.class)
        .extracting(error -> ((ScreenshotImportException) error).getCode())
        .isEqualTo("SCREENSHOT_EMPTY");
  }

  @Test
  void rejectsInvalidImageBytes() {
    MockMultipartFile file =
        new MockMultipartFile("file", "fake.png", "image/png", "not-an-image".getBytes());

    assertThatThrownBy(() -> extractor.extract(file))
        .isInstanceOf(ScreenshotImportException.class)
        .extracting(error -> ((ScreenshotImportException) error).getCode())
        .isEqualTo("SCREENSHOT_IMAGE_INVALID");
  }

  @Test
  void rejectsScreenshotWithoutEnoughReadableText() {
    when(ocr.recognize(any())).thenReturn("短");
    MockMultipartFile file = new MockMultipartFile("file", "blank.png", "image/png", PNG);

    assertThatThrownBy(() -> extractor.extract(file))
        .isInstanceOf(ScreenshotImportException.class)
        .extracting(error -> ((ScreenshotImportException) error).getCode())
        .isEqualTo("SCREENSHOT_NO_TEXT");
  }

  @Test
  void reportsUnavailableOcr() {
    when(ocr.recognize(any())).thenThrow(new WebImportUnavailableException("missing"));
    MockMultipartFile file = new MockMultipartFile("file", "clipboard.png", "image/png", PNG);

    assertThatThrownBy(() -> extractor.extract(file))
        .isInstanceOf(ScreenshotImportException.class)
        .extracting(error -> ((ScreenshotImportException) error).getCode())
        .isEqualTo("SCREENSHOT_IMPORT_UNAVAILABLE");
  }

  @Test
  void rejectsRecognizedTextOverTheLimit() {
    when(ocr.recognize(any())).thenReturn("A".repeat(30_001));
    MockMultipartFile file = new MockMultipartFile("file", "clipboard.png", "image/png", PNG);

    assertThatThrownBy(() -> extractor.extract(file))
        .isInstanceOf(ScreenshotImportException.class)
        .extracting(error -> ((ScreenshotImportException) error).getCode())
        .isEqualTo("SCREENSHOT_TOO_LONG");
  }

  @Test
  void rejectsScreenshotsOverTwentyFiveMebibytesBeforeReading() {
    MultipartFile file = mock(MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(25L * 1024 * 1024 + 1);

    assertThatThrownBy(() -> extractor.extract(file))
        .isInstanceOf(ScreenshotImportException.class)
        .extracting(error -> ((ScreenshotImportException) error).getCode())
        .isEqualTo("SCREENSHOT_TOO_LARGE");
  }

  @Test
  void rejectsAnimatedPngScreenshots() {
    byte[] animated = Arrays.copyOf(PNG, PNG.length + 4);
    System.arraycopy("acTL".getBytes(), 0, animated, PNG.length, 4);
    MockMultipartFile file = new MockMultipartFile("file", "animated.png", "image/png", animated);

    assertThatThrownBy(() -> extractor.extract(file))
        .isInstanceOf(ScreenshotImportException.class)
        .extracting(error -> ((ScreenshotImportException) error).getCode())
        .isEqualTo("SCREENSHOT_IMAGE_INVALID");
  }
}

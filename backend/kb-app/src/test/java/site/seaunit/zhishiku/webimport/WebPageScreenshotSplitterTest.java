package site.seaunit.zhishiku.webimport;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import site.seaunit.zhishiku.richtext.image.ImageInspector;
import site.seaunit.zhishiku.richtext.image.ImportImageCandidate;

class WebPageScreenshotSplitterTest {
  @TempDir Path tempDir;

  private final WebPageScreenshotSplitter splitter = new WebPageScreenshotSplitter();

  @Test
  void keepsAShortScreenshotAsOneCandidate() throws IOException {
    Path screenshot = screenshot(1440, 1500);

    var result = splitter.split(screenshot);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).filename()).isEqualTo("webpage-screenshot.png");
    assertThat(result.get(0).bytes()).isEqualTo(java.nio.file.Files.readAllBytes(screenshot));
  }

  @Test
  void keepsTheProvidedFilenameStemForContentImages() throws IOException {
    Path screenshot = screenshot(1440, 1500);

    var result = splitter.split(screenshot, "content-image-1");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).filename()).isEqualTo("content-image-1.png");
  }

  @Test
  void splitsALongScreenshotIntoOrderedInspectableTiles() throws IOException {
    Path screenshot = screenshot(1440, 12_000);

    var result = splitter.split(screenshot);

    assertThat(result).hasSize(6);
    assertThat(result)
        .extracting(ImportImageCandidate::filename)
        .containsExactly(
            "webpage-screenshot-1.png",
            "webpage-screenshot-2.png",
            "webpage-screenshot-3.png",
            "webpage-screenshot-4.png",
            "webpage-screenshot-5.png",
            "webpage-screenshot-6.png");
    for (ImportImageCandidate tile : result) {
      BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(tile.bytes()));
      assertThat(image.getWidth()).isEqualTo(1440);
      assertThat(image.getHeight()).isLessThanOrEqualTo(ImageInspector.MAX_EDGE);
    }
  }

  @Test
  void rejectsScreenshotsWiderThanTheSafetyLimit() throws IOException {
    Path screenshot = screenshot(ImageInspector.MAX_EDGE + 1, 600);

    assertThat(splitter.split(screenshot)).isEmpty();
  }

  private Path screenshot(int width, int height) throws IOException {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    try {
      graphics.setColor(Color.WHITE);
      graphics.fillRect(0, 0, width, height);
      graphics.setColor(new Color(15, 70, 110));
      graphics.fillRect(width / 4, height / 3, width / 2, Math.max(2, height / 6));
    } finally {
      graphics.dispose();
    }
    Path screenshot = tempDir.resolve("page.png");
    ImageIO.write(image, "png", screenshot.toFile());
    return screenshot;
  }
}

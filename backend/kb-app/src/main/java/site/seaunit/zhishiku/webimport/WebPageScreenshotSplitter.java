package site.seaunit.zhishiku.webimport;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;
import site.seaunit.zhishiku.richtext.image.ImageInspector;
import site.seaunit.zhishiku.richtext.image.ImportImageCandidate;

@Component
public class WebPageScreenshotSplitter {
  private static final int MAX_TILES = 6;
  private static final int PREFERRED_TILE_HEIGHT = 1_800;

  public List<ImportImageCandidate> split(Path screenshot) {
    return split(screenshot, "webpage-screenshot");
  }

  public List<ImportImageCandidate> split(Path screenshot, String filenameStem) {
    try {
      if (!Files.isRegularFile(screenshot)
          || Files.size(screenshot) <= 0
          || Files.size(screenshot) > ImageInspector.MAX_SOURCE_BYTES) {
        return List.of();
      }
      BufferedImage source = ImageIO.read(screenshot.toFile());
      if (source == null
          || source.getWidth() < 1
          || source.getHeight() < 1
          || source.getWidth() > ImageInspector.MAX_EDGE) {
        return List.of();
      }
      if (isInspectable(source.getWidth(), source.getHeight())
          && source.getHeight() <= PREFERRED_TILE_HEIGHT) {
        return List.of(
            new ImportImageCandidate(
                Files.readAllBytes(screenshot), filenameStem + ".png", "image/png"));
      }
      return splitVertically(source, filenameStem);
    } catch (IOException | RuntimeException ignored) {
      // A screenshot failure must not discard text knowledge that is otherwise usable.
      return List.of();
    }
  }

  private List<ImportImageCandidate> splitVertically(BufferedImage source, String filenameStem) {
    int tileCount =
        Math.min(
            MAX_TILES,
            Math.max(1, (source.getHeight() + PREFERRED_TILE_HEIGHT - 1) / PREFERRED_TILE_HEIGHT));
    int tileHeight = (source.getHeight() + tileCount - 1) / tileCount;
    if (!isInspectable(source.getWidth(), tileHeight)) {
      return List.of();
    }
    List<ImportImageCandidate> tiles = new ArrayList<>();
    for (int index = 0; index < tileCount; index++) {
      int y = index * tileHeight;
      int height = Math.min(tileHeight, source.getHeight() - y);
      if (height < 1 || !isInspectable(source.getWidth(), height)) {
        continue;
      }
      byte[] bytes = png(source, y, height);
      if (bytes.length <= 0 || bytes.length > ImageInspector.MAX_SOURCE_BYTES) {
        return List.of();
      }
      tiles.add(
          new ImportImageCandidate(bytes, filenameStem + "-" + (index + 1) + ".png", "image/png"));
    }
    return List.copyOf(tiles);
  }

  private static byte[] png(BufferedImage source, int y, int height) {
    BufferedImage tile = new BufferedImage(source.getWidth(), height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = tile.createGraphics();
    try {
      graphics.drawImage(
          source, 0, y, source.getWidth(), y + height, 0, 0, source.getWidth(), height, null);
    } finally {
      graphics.dispose();
    }
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      if (!ImageIO.write(tile, "png", output)) {
        return new byte[0];
      }
      return output.toByteArray();
    } catch (IOException error) {
      return new byte[0];
    }
  }

  private static boolean isInspectable(int width, int height) {
    return width <= ImageInspector.MAX_EDGE
        && height <= ImageInspector.MAX_EDGE
        && (long) width * height <= ImageInspector.MAX_PIXELS;
  }
}

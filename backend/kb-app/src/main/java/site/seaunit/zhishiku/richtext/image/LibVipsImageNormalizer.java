package site.seaunit.zhishiku.richtext.image;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LibVipsImageNormalizer implements ImageNormalizer {
  private static final long MAX_NORMALIZED_BYTES = 50L * 1024 * 1024;
  private static final Duration TIMEOUT = Duration.ofSeconds(20);

  private final String vipsCommand;

  public LibVipsImageNormalizer(@Value("${app.rich-text.vips-command:vips}") String vipsCommand) {
    this.vipsCommand = vipsCommand;
  }

  @Override
  public byte[] normalize(byte[] source) {
    Path input = null;
    Path output = null;
    try {
      input = Files.createTempFile("zhishiku-image-source-", ".bin");
      output = Files.createTempFile("zhishiku-image-safe-", ".png");
      Files.write(input, source);
      Process process =
          new ProcessBuilder(
                  vipsCommand,
                  "autorot",
                  input.toAbsolutePath().toString(),
                  output.toAbsolutePath() + "[strip]")
              .redirectErrorStream(true)
              .start();
      boolean finished = process.waitFor(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
      if (!finished) {
        process.destroyForcibly();
        throw new ImageInspectionException(
            "IMAGE_NORMALIZER_TIMEOUT", "Image normalization exceeded the time limit.");
      }
      if (process.exitValue() != 0) {
        throw new ImageInspectionException(
            "IMAGE_NORMALIZER_FAILED", "The configured image normalizer rejected the file.");
      }
      long size = Files.size(output);
      if (size <= 0 || size > MAX_NORMALIZED_BYTES) {
        throw new ImageInspectionException(
            "IMAGE_NORMALIZED_SIZE", "Normalized image size is outside the supported range.");
      }
      return Files.readAllBytes(output);
    } catch (IOException error) {
      throw new ImageInspectionException(
          "IMAGE_NORMALIZER_UNAVAILABLE",
          "The configured libvips image normalizer is unavailable.");
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw new ImageInspectionException(
          "IMAGE_NORMALIZER_INTERRUPTED", "Image normalization was interrupted.");
    } finally {
      deleteQuietly(input);
      deleteQuietly(output);
    }
  }

  private static void deleteQuietly(Path path) {
    if (path == null) {
      return;
    }
    try {
      Files.deleteIfExists(path);
    } catch (IOException ignored) {
      // Temporary files contain no durable state and are best-effort cleanup.
    }
  }
}

package site.seaunit.zhishiku.ocr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;
import site.seaunit.zhishiku.config.AppProperties;
import site.seaunit.zhishiku.webimport.WebImportUnavailableException;

@Component
public class TesseractOcrService {
  private static final int MAX_PROCESS_OUTPUT_BYTES = 128 * 1024;

  private final AppProperties.WebImport config;

  public TesseractOcrService(AppProperties properties) {
    this.config = properties.webImport();
  }

  public String recognize(Path screenshot) {
    int timeout = Math.max(10, config.ocrTimeoutSeconds());
    ProcessResult result =
        runProcess(
            List.of(
                config.tesseractCommand(),
                screenshot.toString(),
                "stdout",
                "-l",
                "chi_sim+eng",
                "--psm",
                "3"),
            timeout);
    if (result.exitCode() != 0) {
      throw new WebImportUnavailableException("Image text recognition is unavailable.");
    }
    return result.output().replaceAll("[\\t ]+", " ").replaceAll("\\n{3,}", "\n\n").strip();
  }

  public ProcessResult runProcess(List<String> command, int timeoutSeconds) {
    Process process = null;
    try {
      process = new ProcessBuilder(command).redirectErrorStream(true).start();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      AtomicBoolean outputExceeded = new AtomicBoolean(false);
      AtomicReference<IOException> readFailure = new AtomicReference<>();
      Process readerProcess = process;
      Thread reader =
          new Thread(
              () -> {
                try (InputStream input = readerProcess.getInputStream()) {
                  byte[] buffer = new byte[8192];
                  int read;
                  while ((read = input.read(buffer)) >= 0) {
                    if (outputExceeded.get()) {
                      continue;
                    }
                    if (output.size() + read > MAX_PROCESS_OUTPUT_BYTES) {
                      outputExceeded.set(true);
                      continue;
                    }
                    output.write(buffer, 0, read);
                  }
                } catch (IOException error) {
                  readFailure.set(error);
                }
              },
              "zhishiku-process-output");
      reader.setDaemon(true);
      reader.start();
      boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
        reader.join(1_000);
        throw new WebImportUnavailableException("Image processing timed out.");
      }
      reader.join(5_000);
      if (reader.isAlive()) {
        throw new WebImportUnavailableException("Image processing output did not close.");
      }
      if (readFailure.get() != null) {
        throw new WebImportUnavailableException("Image processing output is unavailable.");
      }
      if (outputExceeded.get()) {
        throw new WebImportUnavailableException("Image processing output exceeded the limit.");
      }
      return new ProcessResult(process.exitValue(), output.toString(StandardCharsets.UTF_8));
    } catch (IOException error) {
      throw new WebImportUnavailableException("Required OCR tool is unavailable.");
    } catch (InterruptedException error) {
      if (process != null) {
        process.destroyForcibly();
      }
      Thread.currentThread().interrupt();
      throw new WebImportUnavailableException("Image processing was interrupted.");
    }
  }

  public record ProcessResult(int exitCode, String output) {}
}

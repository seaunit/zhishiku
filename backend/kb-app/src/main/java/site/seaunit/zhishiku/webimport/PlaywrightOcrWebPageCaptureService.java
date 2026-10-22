package site.seaunit.zhishiku.webimport;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import site.seaunit.zhishiku.config.AppProperties;
import site.seaunit.zhishiku.ocr.TesseractOcrService;
import site.seaunit.zhishiku.richtext.provenance.SourceProvenancePolicy;

@Component
public class PlaywrightOcrWebPageCaptureService implements WebPageCaptureGateway {
  private static final long MAX_SCREENSHOT_BYTES = 50L * 1024 * 1024;

  private final AppProperties.WebImport config;
  private final SourceProvenancePolicy sourcePolicy;
  private final TesseractOcrService ocr;

  public PlaywrightOcrWebPageCaptureService(
      AppProperties properties, SourceProvenancePolicy sourcePolicy, TesseractOcrService ocr) {
    this.config = properties.webImport();
    this.sourcePolicy = sourcePolicy;
    this.ocr = ocr;
  }

  @Override
  public CapturedWebPage capture(String requestedUrl) {
    String url = sourcePolicy.sanitizeUrl(requestedUrl);
    requirePublicResolvedHost(url);
    Path screenshot = null;
    Path metadata = null;
    try {
      screenshot = Files.createTempFile("zhishiku-webpage-", ".png");
      metadata = Files.createTempFile("zhishiku-webpage-", ".url");
      String finalUrl = runCapture(url, screenshot, metadata);
      String sanitizedFinalUrl = sourcePolicy.sanitizeUrl(finalUrl);
      requirePublicResolvedHost(sanitizedFinalUrl);
      String text = ocr.recognize(screenshot);
      if (text.length() < 80) {
        throw new WebPageNoTextException("The rendered page did not contain enough readable text.");
      }
      int limit = Math.max(1000, config.maxTextChars());
      return new CapturedWebPage(
          url, sanitizedFinalUrl, text.substring(0, Math.min(text.length(), limit)));
    } catch (IOException error) {
      throw new WebImportUnavailableException("Web page capture is unavailable.");
    } finally {
      if (screenshot != null) {
        try {
          Files.deleteIfExists(screenshot);
        } catch (IOException ignored) {
          // Temporary screenshot cleanup is best effort.
        }
      }
      if (metadata != null) {
        try {
          Files.deleteIfExists(metadata);
        } catch (IOException ignored) {
          // Temporary metadata cleanup is best effort.
        }
      }
    }
  }

  private String runCapture(String url, Path screenshot, Path metadata) {
    Path workerJar = Path.of(config.workerJar());
    if (!Files.isRegularFile(workerJar)) {
      throw new WebImportUnavailableException("Browser worker is not installed.");
    }
    int timeout = Math.max(10, config.browserTimeoutSeconds());
    TesseractOcrService.ProcessResult result =
        ocr.runProcess(
            List.of(
                "java",
                "-Xms32m",
                "-Xmx320m",
                "-jar",
                workerJar.toString(),
                "--capture",
                url,
                "--output",
                screenshot.toString(),
                "--metadata",
                metadata.toString()),
            timeout);
    if (result.exitCode() != 0) {
      throw new WebImportUnavailableException("The page could not be rendered.");
    }
    try {
      long size = Files.size(screenshot);
      if (size <= 0 || size > MAX_SCREENSHOT_BYTES) {
        throw new WebImportUnavailableException("Rendered screenshot size is invalid.");
      }
    } catch (IOException error) {
      throw new WebImportUnavailableException("Rendered screenshot is unavailable.");
    }
    try {
      String finalUrl = Files.readString(metadata, StandardCharsets.UTF_8).strip();
      if (finalUrl.isBlank()) {
        throw new WebImportUnavailableException("Rendered page URL is unavailable.");
      }
      return finalUrl;
    } catch (IOException error) {
      throw new WebImportUnavailableException("Rendered page URL is unavailable.");
    }
  }

  private static void requirePublicResolvedHost(String url) {
    try {
      URI uri = URI.create(url);
      InetAddress[] addresses = InetAddress.getAllByName(uri.getHost());
      boolean allPublic =
          Arrays.stream(addresses).allMatch(PlaywrightOcrWebPageCaptureService::isPublicAddress);
      if (!allPublic) {
        throw new IllegalArgumentException("Source URL must target a public host.");
      }
    } catch (IOException error) {
      throw new IllegalArgumentException("Source URL host could not be resolved.");
    }
  }

  private static boolean isPublicAddress(InetAddress address) {
    if (address.isAnyLocalAddress()
        || address.isLoopbackAddress()
        || address.isLinkLocalAddress()
        || address.isSiteLocalAddress()
        || address.isMulticastAddress()) {
      return false;
    }
    byte[] bytes = address.getAddress();
    if (bytes.length == 4) {
      int first = Byte.toUnsignedInt(bytes[0]);
      int second = Byte.toUnsignedInt(bytes[1]);
      int third = Byte.toUnsignedInt(bytes[2]);
      if (first == 0
          || (first == 100 && second >= 64 && second <= 127)
          || (first == 192 && second == 0 && (third == 0 || third == 2))
          || (first == 198 && (second == 18 || second == 19 || second == 51))
          || (first == 203 && second == 0 && third == 113)
          || first >= 240) {
        return false;
      }
    }
    if (bytes.length == 16) {
      int first = Byte.toUnsignedInt(bytes[0]);
      int second = Byte.toUnsignedInt(bytes[1]);
      if ((first & 0xfe) == 0xfc || (first == 0xfe && (second & 0xc0) == 0x80)) {
        return false;
      }
      if (first == 0x20
          && second == 0x01
          && Byte.toUnsignedInt(bytes[2]) == 0x0d
          && Byte.toUnsignedInt(bytes[3]) == 0xb8) {
        return false;
      }
    }
    return true;
  }
}

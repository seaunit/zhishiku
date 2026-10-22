package site.seaunit.zhishiku.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.CLI;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.ScreenshotType;
import com.microsoft.playwright.options.WaitUntilState;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class WebPageCaptureWorker {
  private static final int VIEWPORT_WIDTH = 1440;
  private static final int VIEWPORT_HEIGHT = 1200;
  private static final int MAX_SCREENSHOT_HEIGHT = 12_000;
  private static final int NAVIGATION_TIMEOUT_MS = 25_000;

  private WebPageCaptureWorker() {}

  public static void main(String[] arguments) {
    if (arguments.length == 1 && "--install-browser".equals(arguments[0])) {
      try {
        CLI.main(new String[] {"install", "--with-deps", "chromium"});
      } catch (java.io.IOException error) {
        fail("Chromium installation failed.");
      } catch (InterruptedException error) {
        Thread.currentThread().interrupt();
        fail("Chromium installation was interrupted.");
      }
      return;
    }
    if (arguments.length != 6 || !"--capture".equals(arguments[0])) {
      fail(
          "Usage: WebPageCaptureWorker --capture <url> --output <screenshot-path> "
              + "--metadata <final-url-path>");
    }
    String url = arguments[1];
    Path screenshot = Path.of(arguments[3]);
    Path metadata = Path.of(arguments[5]);
    if (!"--output".equals(arguments[2])) {
      fail("Screenshot option is required.");
    }
    if (!"--metadata".equals(arguments[4])) {
      fail("Metadata option is required.");
    }
    try {
      Files.createDirectories(screenshot.toAbsolutePath().getParent());
      Files.createDirectories(metadata.toAbsolutePath().getParent());
      String finalUrl = capture(url, screenshot);
      Files.writeString(metadata, finalUrl);
    } catch (java.io.IOException error) {
      fail("Screenshot output directory is unavailable.");
    } catch (PlaywrightException error) {
      fail("Browser capture failed: " + safeMessage(error));
    } catch (RuntimeException error) {
      fail(safeMessage(error));
    }
  }

  private static String capture(String url, Path screenshot) {
    try (Playwright playwright = Playwright.create();
        Browser browser =
            playwright
                .chromium()
                .launch(
                    new BrowserType.LaunchOptions()
                        .setHeadless(true)
                        .setArgs(
                            Arrays.asList(
                                "--disable-dev-shm-usage", "--disable-gpu", "--hide-scrollbars")));
        BrowserContext context =
            browser.newContext(
                new Browser.NewContextOptions()
                    .setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
                    .setAcceptDownloads(false)
                    .setJavaScriptEnabled(true));
        Page page = context.newPage()) {
      page.setDefaultTimeout(NAVIGATION_TIMEOUT_MS);
      page.route(
          "**/*",
          route -> {
            String requestUrl = route.request().url();
            String scheme = requestUrl.split(":", 2)[0].toLowerCase();
            if ((scheme.equals("http") || scheme.equals("https"))
                && targetsPublicHost(requestUrl)) {
              route.resume();
              return;
            }
            route.abort();
          });
      page.navigate(
          url,
          new Page.NavigateOptions()
              .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
              .setTimeout(NAVIGATION_TIMEOUT_MS));
      page.waitForTimeout(1_500);
      double pageHeight =
          ((Number)
                  page.evaluate(
                      "() => Math.max(document.body?.scrollHeight || 0, document.documentElement?.scrollHeight || 0, 1200)"))
              .doubleValue();
      double clipHeight = Math.min(pageHeight, MAX_SCREENSHOT_HEIGHT);
      page.screenshot(
          new Page.ScreenshotOptions()
              .setPath(screenshot)
              .setType(ScreenshotType.PNG)
              .setClip(0, 0, VIEWPORT_WIDTH, clipHeight));
      if (!Files.isRegularFile(screenshot) || Files.size(screenshot) <= 0) {
        throw new IllegalStateException("Screenshot output is empty.");
      }
      return page.url();
    } catch (java.io.IOException error) {
      throw new IllegalStateException("Screenshot output is not readable.", error);
    }
  }

  static boolean targetsPublicHost(String value) {
    try {
      URI uri = URI.create(value);
      if (uri.getHost() == null || uri.getHost().isBlank()) {
        return false;
      }
      InetAddress[] addresses = InetAddress.getAllByName(uri.getHost());
      return addresses.length > 0
          && Arrays.stream(addresses).allMatch(WebPageCaptureWorker::isPublicAddress);
    } catch (RuntimeException | java.io.IOException error) {
      return false;
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

  private static String safeMessage(Throwable error) {
    String message = error.getMessage();
    if (message == null || message.isBlank()) {
      return error.getClass().getSimpleName();
    }
    return message.replaceAll("[\\r\\n\\t]+", " ").strip();
  }

  private static void fail(String message) {
    System.err.println(message);
    System.exit(2);
  }
}

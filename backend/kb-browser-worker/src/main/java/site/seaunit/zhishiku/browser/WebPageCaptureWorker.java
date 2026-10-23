package site.seaunit.zhishiku.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.CLI;
import com.microsoft.playwright.Locator;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WebPageCaptureWorker {
  private static final int VIEWPORT_WIDTH = 1440;
  private static final int VIEWPORT_HEIGHT = 1200;
  private static final int MAX_SCREENSHOT_HEIGHT = 12_000;
  private static final int NAVIGATION_TIMEOUT_MS = 25_000;
  private static final int MAX_CONTENT_IMAGES = 6;
  private static final int MIN_CONTENT_IMAGE_WIDTH = 240;
  private static final int MIN_CONTENT_IMAGE_HEIGHT = 120;
  private static final long MIN_CONTENT_IMAGE_PIXELS = 40_000;
  private static final int MAX_CONTENT_IMAGE_EDGE = 8_192;
  private static final long MAX_CONTENT_IMAGE_PIXELS = 40_000_000;

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
    if (arguments.length != 8 || !"--capture".equals(arguments[0])) {
      fail(
          "Usage: WebPageCaptureWorker --capture <url> --output <screenshot-path> "
              + "--metadata <final-url-path> --images <image-output-directory>");
    }
    String url = arguments[1];
    Path screenshot = Path.of(arguments[3]);
    Path metadata = Path.of(arguments[5]);
    Path images = Path.of(arguments[7]);
    if (!"--output".equals(arguments[2])) {
      fail("Screenshot option is required.");
    }
    if (!"--metadata".equals(arguments[4])) {
      fail("Metadata option is required.");
    }
    if (!"--images".equals(arguments[6])) {
      fail("Image output option is required.");
    }
    try {
      Files.createDirectories(screenshot.toAbsolutePath().getParent());
      Files.createDirectories(metadata.toAbsolutePath().getParent());
      Files.createDirectories(images);
      String finalUrl = capture(url, screenshot, images);
      Files.writeString(metadata, finalUrl);
    } catch (java.io.IOException error) {
      fail("Screenshot output directory is unavailable.");
    } catch (PlaywrightException error) {
      fail("Browser capture failed: " + safeMessage(error));
    } catch (RuntimeException error) {
      fail(safeMessage(error));
    }
  }

  private static String capture(String url, Path screenshot, Path images) {
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
      captureContentImages(page, images);
      if (!Files.isRegularFile(screenshot) || Files.size(screenshot) <= 0) {
        throw new IllegalStateException("Screenshot output is empty.");
      }
      return page.url();
    } catch (java.io.IOException error) {
      throw new IllegalStateException("Screenshot output is not readable.", error);
    }
  }

  @SuppressWarnings("unchecked")
  private static void captureContentImages(Page page, Path outputDirectory)
      throws java.io.IOException {
    Object evaluated =
        page.evaluate(
            """
            () => {
              const result = [];
              const nodes = Array.from(
                document.querySelectorAll('img, svg, canvas')
              );
              nodes.forEach((element, index) => {
                const rect = element.getBoundingClientRect();
                const style = getComputedStyle(element);
                const key = 'visual-' + index;
                element.setAttribute('data-shihai-visual-key', key);
                result.push({
                  key,
                  left: Math.round(rect.left + window.scrollX),
                  top: Math.round(rect.top + window.scrollY),
                  width: Math.round(rect.width),
                  height: Math.round(rect.height),
                  navigation: Boolean(
                    element.closest(
                      'nav, header, footer, aside, [role="navigation"], [role="banner"], ' +
                      '[role="contentinfo"], [role="complementary"], [aria-hidden="true"]'
                    )
                  ) ||
                    style.position === 'fixed' ||
                    (
                      element.tagName.toLowerCase() === 'canvas' &&
                      rect.width >= window.innerWidth * 0.85 &&
                      rect.height >= window.innerHeight * 0.6
                    ) ||
                    (
                      rect.left >= window.innerWidth * 0.65 &&
                      rect.width <= window.innerWidth * 0.4
                    ),
                  sourceKey:
                    element instanceof HTMLImageElement
                      ? (element.currentSrc || element.src || '')
                      : '',
                  visible:
                    style.display !== 'none' &&
                    style.visibility !== 'hidden' &&
                    Number(style.opacity || '1') > 0
                });
              });
              return result;
            }
            """);
    if (!(evaluated instanceof List<?> values)) {
      return;
    }
    List<VisualElement> candidates =
        values.stream()
            .filter(Map.class::isInstance)
            .map(value -> (Map<String, Object>) value)
            .filter(value -> Boolean.TRUE.equals(value.get("visible")))
            .map(WebPageCaptureWorker::visualElement)
            .toList();
    List<VisualElement> selected = selectVisualElements(candidates);
    for (int index = 0; index < selected.size(); index++) {
      VisualElement element = selected.get(index);
      Path output = outputDirectory.resolve("content-image-" + (index + 1) + ".png");
      try {
        Locator locator = page.locator("[data-shihai-visual-key='" + element.key() + "']");
        locator.scrollIntoViewIfNeeded();
        page.waitForTimeout(250);
        locator.screenshot(
            new Locator.ScreenshotOptions().setPath(output).setType(ScreenshotType.PNG));
      } catch (PlaywrightException ignored) {
        Files.deleteIfExists(output);
      }
    }
  }

  private static VisualElement visualElement(Map<String, Object> value) {
    return new VisualElement(
        String.valueOf(value.get("key")),
        number(value.get("left")),
        number(value.get("top")),
        number(value.get("width")),
        number(value.get("height")),
        Boolean.TRUE.equals(value.get("navigation")),
        String.valueOf(value.getOrDefault("sourceKey", "")));
  }

  private static int number(Object value) {
    return value instanceof Number number ? number.intValue() : 0;
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

  static List<VisualElement> selectVisualElements(List<VisualElement> elements) {
    List<VisualElement> eligible =
        elements.stream()
            .filter(element -> !element.navigation())
            .filter(element -> element.width() >= MIN_CONTENT_IMAGE_WIDTH)
            .filter(element -> element.height() >= MIN_CONTENT_IMAGE_HEIGHT)
            .filter(
                element -> (long) element.width() * element.height() >= MIN_CONTENT_IMAGE_PIXELS)
            .filter(
                element ->
                    element.width() <= MAX_CONTENT_IMAGE_EDGE
                        && element.height() <= MAX_CONTENT_IMAGE_EDGE)
            .filter(
                element -> (long) element.width() * element.height() <= MAX_CONTENT_IMAGE_PIXELS)
            .sorted(
                java.util.Comparator.comparingLong(
                        (VisualElement element) -> (long) element.width() * element.height())
                    .reversed()
                    .thenComparingInt(VisualElement::top)
                    .thenComparingInt(VisualElement::left))
            .toList();
    List<VisualElement> selected = new java.util.ArrayList<>();
    Set<String> sourceKeys = new HashSet<>();
    for (VisualElement element : eligible) {
      if (!element.sourceKey().isBlank() && !sourceKeys.add(element.sourceKey())) {
        continue;
      }
      boolean overlapsExisting =
          selected.stream().anyMatch(existing -> overlapRatio(existing, element) >= 0.8);
      if (!overlapsExisting) {
        selected.add(element);
      }
      if (selected.size() >= MAX_CONTENT_IMAGES) {
        break;
      }
    }
    return List.copyOf(selected);
  }

  private static double overlapRatio(VisualElement first, VisualElement second) {
    long left = Math.max(first.left(), second.left());
    long top = Math.max(first.top(), second.top());
    long right =
        Math.min(first.left() + (long) first.width(), second.left() + (long) second.width());
    long bottom =
        Math.min(first.top() + (long) first.height(), second.top() + (long) second.height());
    long intersection = Math.max(0, right - left) * Math.max(0, bottom - top);
    long smaller =
        Math.min((long) first.width() * first.height(), (long) second.width() * second.height());
    return smaller == 0 ? 0 : (double) intersection / smaller;
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

  record VisualElement(
      String key, int left, int top, int width, int height, boolean navigation, String sourceKey) {}
}

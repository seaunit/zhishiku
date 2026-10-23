package site.seaunit.zhishiku.richtext.image;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ImageInspector {
  public static final long MAX_PIXELS = 40_000_000L;
  public static final int MAX_EDGE = 8_192;
  public static final long MAX_SOURCE_BYTES = 25L * 1024 * 1024;
  private static final List<Pattern> SECRET_PATTERNS =
      List.of(
          Pattern.compile("AKIA[0-9A-Z]{16}"),
          Pattern.compile("-----BEGIN [A-Z ]*PRIVATE KEY-----"),
          Pattern.compile("sk-[A-Za-z0-9_-]{20,}"),
          Pattern.compile("(?i)(password|secret|api[_-]?key)\\s*[:=]\\s*\\S{8,}"));

  public ImageInspection inspect(byte[] bytes) {
    ImageInspection inspection = detect(bytes);
    if (inspection.width() < 1 || inspection.height() < 1) {
      throw invalid("IMAGE_DIMENSIONS", "Image dimensions could not be read.");
    }
    if (inspection.width() > MAX_EDGE || inspection.height() > MAX_EDGE) {
      throw invalid("IMAGE_EDGE_LIMIT", "Image exceeds the 8192 pixel edge limit.");
    }
    if ((long) inspection.width() * inspection.height() > MAX_PIXELS) {
      throw invalid("IMAGE_PIXEL_LIMIT", "Image exceeds the 40 million pixel limit.");
    }
    String raw = new String(bytes, StandardCharsets.ISO_8859_1);
    if (SECRET_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(raw).find())) {
      throw invalid(
          "IMAGE_SECRET_DETECTED",
          "A possible secret was detected in the image. Remove it and upload a redacted copy.");
    }
    return inspection;
  }

  private ImageInspection detect(byte[] bytes) {
    if (isPng(bytes)) {
      return new ImageInspection("image/png", readInt(bytes, 16), readInt(bytes, 20), "png");
    }
    if (isJpeg(bytes)) {
      int[] size = jpegSize(bytes);
      return new ImageInspection("image/jpeg", size[0], size[1], "jpg");
    }
    if (isWebp(bytes)) {
      int[] size = webpSize(bytes);
      return new ImageInspection("image/webp", size[0], size[1], "webp");
    }
    if (isHeic(bytes)) {
      throw invalid(
          "IMAGE_NORMALIZER_REQUIRED",
          "HEIC/HEIF requires the configured libvips normalizer and is unavailable in this MVP.");
    }
    throw invalid(
        "UNSUPPORTED_IMAGE_FORMAT", "Only JPEG, PNG, WebP and normalized HEIC are accepted.");
  }

  private static boolean isPng(byte[] b) {
    return b.length > 24 && (b[0] & 0xff) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
  }

  private static boolean isJpeg(byte[] b) {
    return b.length > 4 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xd8;
  }

  private static boolean isWebp(byte[] b) {
    return b.length > 30
        && b[0] == 'R'
        && b[1] == 'I'
        && b[2] == 'F'
        && b[3] == 'F'
        && b[8] == 'W'
        && b[9] == 'E'
        && b[10] == 'B'
        && b[11] == 'P';
  }

  private static boolean isHeic(byte[] b) {
    if (b.length < 12) {
      return false;
    }
    String box = new String(b, 4, 4, StandardCharsets.ISO_8859_1);
    return "ftyp".equals(box)
        && (new String(b, 8, Math.min(12, b.length - 8), StandardCharsets.ISO_8859_1)
            .contains("hei"));
  }

  private static int[] webpSize(byte[] b) {
    String chunk = new String(b, 12, 4, StandardCharsets.ISO_8859_1);
    if ("VP8X".equals(chunk)) {
      if ((b[20] & 0x02) != 0) {
        throw invalid("ANIMATED_IMAGE", "Animated WebP images are not accepted.");
      }
      int width = 1 + read24(b, 24);
      int height = 1 + read24(b, 27);
      return new int[] {width, height};
    }
    if ("VP8 ".equals(chunk)) {
      int width = read16(b, 26) & 0x3fff;
      int height = read16(b, 28) & 0x3fff;
      return new int[] {width, height};
    }
    if ("VP8L".equals(chunk)) {
      int bits = readInt(b, 21);
      int width = (bits & 0x3fff) + 1;
      int height = ((bits >> 14) & 0x3fff) + 1;
      return new int[] {width, height};
    }
    throw invalid("WEBP_DIMENSIONS", "Unsupported WebP encoding.");
  }

  private static int[] jpegSize(byte[] b) {
    int offset = 2;
    while (offset + 9 < b.length) {
      if ((b[offset] & 0xff) != 0xff) {
        offset++;
        continue;
      }
      int marker = b[offset + 1] & 0xff;
      offset += 2;
      if (marker == 0xd8 || marker == 0xd9) {
        continue;
      }
      if (offset + 2 > b.length) {
        break;
      }
      int length = read16(b, offset);
      if (length < 2 || offset + length > b.length) {
        break;
      }
      if ((marker >= 0xc0 && marker <= 0xc3)
          || (marker >= 0xc5 && marker <= 0xc7)
          || (marker >= 0xc9 && marker <= 0xcb)
          || (marker >= 0xcd && marker <= 0xcf)) {
        return new int[] {read16(b, offset + 5), read16(b, offset + 3)};
      }
      offset += length;
    }
    throw invalid("JPEG_DIMENSIONS", "JPEG dimensions could not be read.");
  }

  private static int readInt(byte[] b, int offset) {
    if (offset + 4 > b.length) {
      throw invalid("IMAGE_TRUNCATED", "Image header is incomplete.");
    }
    return ((b[offset] & 0xff) << 24)
        | ((b[offset + 1] & 0xff) << 16)
        | ((b[offset + 2] & 0xff) << 8)
        | (b[offset + 3] & 0xff);
  }

  private static int read24(byte[] b, int offset) {
    return (b[offset] & 0xff) | ((b[offset + 1] & 0xff) << 8) | ((b[offset + 2] & 0xff) << 16);
  }

  private static int read16(byte[] b, int offset) {
    if (offset + 2 > b.length) {
      throw invalid("IMAGE_TRUNCATED", "Image header is incomplete.");
    }
    return ((b[offset] & 0xff) << 8) | (b[offset + 1] & 0xff);
  }

  private static ImageInspectionException invalid(String code, String message) {
    return new ImageInspectionException(code, message);
  }
}

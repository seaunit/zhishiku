package site.seaunit.zhishiku.richtext.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class ImageInspectorTest {
  private static final byte[] PNG =
      Base64.getDecoder()
          .decode(
              "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9ZQmcAAAAASUVORK5CYII=");
  private final ImageInspector inspector = new ImageInspector();

  @Test
  void acceptsPngAndReadsDimensions() {
    ImageInspection result = inspector.inspect(PNG);

    assertThat(result.mediaType()).isEqualTo("image/png");
    assertThat(result.width()).isEqualTo(1);
    assertThat(result.height()).isEqualTo(1);
  }

  @Test
  void rejectsSvgAndFilesContainingSecretPatterns() {
    assertThatThrownBy(() -> inspector.inspect("<svg></svg>".getBytes()))
        .isInstanceOf(ImageInspectionException.class)
        .hasMessageContaining("Only JPEG");

    byte[] unsafe = PNG.clone();
    byte[] secret = "AKIAABCDEFGHIJKLMNOP".getBytes();
    byte[] combined = new byte[PNG.length + secret.length];
    System.arraycopy(PNG, 0, combined, 0, PNG.length);
    System.arraycopy(secret, 0, combined, PNG.length, secret.length);
    assertThatThrownBy(() -> inspector.inspect(combined))
        .isInstanceOf(ImageInspectionException.class)
        .hasMessageContaining("secret");
  }

  @Test
  void rejectsEdgeAndDecodedPixelLimitsBeforeUpload() {
    assertThatThrownBy(() -> inspector.inspect(pngWithSize(8_193, 1)))
        .isInstanceOf(ImageInspectionException.class)
        .extracting(error -> ((ImageInspectionException) error).getCode())
        .isEqualTo("IMAGE_EDGE_LIMIT");

    assertThatThrownBy(() -> inspector.inspect(pngWithSize(8_000, 5_001)))
        .isInstanceOf(ImageInspectionException.class)
        .extracting(error -> ((ImageInspectionException) error).getCode())
        .isEqualTo("IMAGE_PIXEL_LIMIT");

    assertThat(inspector.inspect(pngWithSize(8_000, 5_000)).width()).isEqualTo(8_000);
  }

  @Test
  void rejectsHeicUntilTheConfiguredNormalizerHandlesIt() {
    byte[] heic = new byte[32];
    System.arraycopy("....ftypheic".getBytes(), 0, heic, 0, 12);

    assertThatThrownBy(() -> inspector.inspect(heic))
        .isInstanceOf(ImageInspectionException.class)
        .extracting(error -> ((ImageInspectionException) error).getCode())
        .isEqualTo("IMAGE_NORMALIZER_REQUIRED");
  }

  private static byte[] pngWithSize(int width, int height) {
    byte[] image = PNG.clone();
    ByteBuffer.wrap(image).putInt(16, width).putInt(20, height);
    return image;
  }
}

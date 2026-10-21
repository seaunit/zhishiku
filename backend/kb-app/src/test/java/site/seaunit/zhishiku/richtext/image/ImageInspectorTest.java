package site.seaunit.zhishiku.richtext.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
}

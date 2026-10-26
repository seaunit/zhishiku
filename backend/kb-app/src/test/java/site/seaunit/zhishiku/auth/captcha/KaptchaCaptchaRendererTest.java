package site.seaunit.zhishiku.auth.captcha;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class KaptchaCaptchaRendererTest {
  @Test
  void rendersAFiveCharacterPng() throws Exception {
    CaptchaRendered rendered = new KaptchaCaptchaRenderer().render();

    assertThat(rendered.answer()).hasSize(5).matches("[2-9A-HJ-NP-Z]{5}");
    assertThat(rendered.png()).startsWith((byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G');
    assertThat(ImageIO.read(new ByteArrayInputStream(rendered.png()))).isNotNull();
  }

  @Test
  void keepsSaltAndProducesDistinctReadableImages() {
    KaptchaCaptchaRenderer renderer = new KaptchaCaptchaRenderer();

    CaptchaRendered first = renderer.render();
    CaptchaRendered second = renderer.render();

    assertThat(first.png()).isNotEqualTo(second.png());
    assertThat(first.answer()).isNotEqualTo(second.answer());
  }
}

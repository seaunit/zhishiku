package site.seaunit.zhishiku.auth.captcha;

import com.google.code.kaptcha.Constants;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

@Component
public class KaptchaCaptchaRenderer implements CaptchaRenderer {
  private final DefaultKaptcha kaptcha;

  public KaptchaCaptchaRenderer() {
    Properties properties = new Properties();
    properties.setProperty(Constants.KAPTCHA_IMAGE_WIDTH, "150");
    properties.setProperty(Constants.KAPTCHA_IMAGE_HEIGHT, "48");
    properties.setProperty(Constants.KAPTCHA_BORDER, "no");
    properties.setProperty(Constants.KAPTCHA_TEXTPRODUCER_CHAR_LENGTH, "5");
    properties.setProperty(
        Constants.KAPTCHA_TEXTPRODUCER_CHAR_STRING, "23456789ABCDEFGHJKLMNPQRSTUVWXYZ");
    properties.setProperty(Constants.KAPTCHA_TEXTPRODUCER_FONT_SIZE, "28");
    properties.setProperty(Constants.KAPTCHA_TEXTPRODUCER_FONT_COLOR, "31,45,51");
    properties.setProperty(Constants.KAPTCHA_NOISE_COLOR, "205,216,221");
    properties.setProperty(Constants.KAPTCHA_BACKGROUND_CLR_FROM, "246,249,250");
    properties.setProperty(Constants.KAPTCHA_BACKGROUND_CLR_TO, "246,249,250");
    kaptcha = new DefaultKaptcha();
    kaptcha.setConfig(new Config(properties));
  }

  @Override
  public CaptchaRendered render() {
    String answer = kaptcha.createText();
    BufferedImage image = kaptcha.createImage(answer);
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      ImageIO.write(image, "png", output);
      return new CaptchaRendered(answer, output.toByteArray());
    } catch (IOException error) {
      throw new IllegalStateException("Could not render captcha image.", error);
    }
  }
}

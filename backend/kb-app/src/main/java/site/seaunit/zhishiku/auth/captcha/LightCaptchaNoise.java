package site.seaunit.zhishiku.auth.captcha;

import com.google.code.kaptcha.NoiseProducer;
import com.google.code.kaptcha.util.Configurable;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.util.Random;

public class LightCaptchaNoise extends Configurable implements NoiseProducer {
  private static final int LINE_COUNT = 2;

  @Override
  public void makeNoise(
      BufferedImage image, float factorOne, float factorTwo, float factorThree, float factorFour) {
    int width = image.getWidth();
    int height = image.getHeight();
    Color base = getConfig().getNoiseColor();
    Color light = new Color(base.getRed(), base.getGreen(), base.getBlue(), 90);
    Random random = new Random();
    Graphics2D graphics = image.createGraphics();
    try {
      graphics.setRenderingHint(
          RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.setColor(light);
      graphics.setStroke(new BasicStroke(0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      for (int index = 0; index < LINE_COUNT; index++) {
        double startY = height * (0.15 + random.nextDouble() * 0.25);
        double endY = height * (0.60 + random.nextDouble() * 0.25);
        double controlY = height * (0.25 + random.nextDouble() * 0.50);
        graphics.draw(
            new QuadCurve2D.Double(
                -4, startY, width * 0.5, controlY, width + 4, endY));
      }
    } finally {
      graphics.dispose();
    }
  }
}

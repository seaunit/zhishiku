package site.seaunit.zhishiku.config;

import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import site.seaunit.zhishiku.auth.captcha.CaptchaRenderer;
import site.seaunit.zhishiku.auth.captcha.CaptchaService;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class ApplicationConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  CaptchaService captchaService(CaptchaRenderer renderer, AppProperties properties, Clock clock) {
    AppProperties.Captcha captcha = properties.captcha();
    return new CaptchaService(
        renderer, clock, Duration.ofSeconds(captcha.ttlSeconds()), captcha.maxEntries());
  }
}

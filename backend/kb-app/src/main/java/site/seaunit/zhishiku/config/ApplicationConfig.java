package site.seaunit.zhishiku.config;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import site.seaunit.zhishiku.ai.orchestration.AiOrchestrationProperties;
import site.seaunit.zhishiku.auth.captcha.CaptchaRenderer;
import site.seaunit.zhishiku.auth.captcha.CaptchaService;
import site.seaunit.zhishiku.auth.registration.EmailAddressPolicy;
import site.seaunit.zhishiku.auth.registration.EmailVerificationService;
import site.seaunit.zhishiku.auth.registration.LocalRegistrationEmailSender;
import site.seaunit.zhishiku.auth.registration.RegistrationEmailSender;
import site.seaunit.zhishiku.auth.registration.SmtpRegistrationEmailSender;
import site.seaunit.zhishiku.owner.OwnerService;

@Configuration
@EnableConfigurationProperties({AppProperties.class, AiOrchestrationProperties.class})
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

  @Bean
  EmailAddressPolicy emailAddressPolicy() {
    return EmailAddressPolicy.fromResource("security/disposable-email-domains.txt");
  }

  @Bean
  @Profile("!local")
  RegistrationEmailSender smtpRegistrationEmailSender(
      JavaMailSender mailSender, AppProperties properties) {
    return new SmtpRegistrationEmailSender(
        mailSender, properties.registration().fromAddress(), properties.registration().fromName());
  }

  @Bean
  @Profile("local")
  RegistrationEmailSender localRegistrationEmailSender() {
    return new LocalRegistrationEmailSender();
  }

  @Bean
  EmailVerificationService emailVerificationService(
      RegistrationEmailSender sender,
      EmailAddressPolicy emailPolicy,
      OwnerService ownerService,
      AppProperties properties,
      Clock clock) {
    AppProperties.Registration registration = properties.registration();
    SecureRandom random = new SecureRandom();
    Supplier<String> codeSupplier = () -> String.valueOf(100000 + random.nextInt(900000));
    return new EmailVerificationService(
        sender,
        emailPolicy,
        ownerService::isRegistrationOpen,
        clock,
        codeSupplier,
        new EmailVerificationService.Config(
            Duration.ofSeconds(registration.codeTtlSeconds()),
            Duration.ofSeconds(registration.resendCooldownSeconds()),
            Duration.ofSeconds(registration.sendWindowSeconds()),
            registration.maxSendsPerEmail(),
            registration.maxSendsPerIp(),
            registration.maxAttempts(),
            registration.maxCachedCodes()));
  }
}

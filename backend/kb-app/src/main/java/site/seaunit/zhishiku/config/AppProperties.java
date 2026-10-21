package site.seaunit.zhishiku.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    Owner owner, Storage storage, Security security, Captcha captcha, Registration registration) {
  public record Owner(
      String email, String password, String bootstrapToken, boolean autoBootstrap) {}

  public record Storage(String root) {}

  public record Security(String cookieName, String cookiePath) {}

  public record Captcha(long ttlSeconds, long maxEntries, boolean exposeAnswer) {}

  public record Registration(
      boolean enabled,
      long codeTtlSeconds,
      long resendCooldownSeconds,
      long sendWindowSeconds,
      int maxSendsPerEmail,
      int maxSendsPerIp,
      int maxAttempts,
      long maxCachedCodes,
      boolean exposeCode,
      String fromAddress,
      String fromName) {}
}

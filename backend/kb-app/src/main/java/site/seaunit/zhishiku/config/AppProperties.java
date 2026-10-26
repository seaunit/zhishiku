package site.seaunit.zhishiku.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    Owner owner,
    Storage storage,
    Security security,
    Captcha captcha,
    Registration registration,
    ExternalSearch externalSearch,
    Ai ai,
    WebImport webImport,
    Admin admin,
    Billing billing) {
  @ConstructorBinding
  public AppProperties {}

  public AppProperties(
      Owner owner,
      Storage storage,
      Security security,
      Captcha captcha,
      Registration registration,
      ExternalSearch externalSearch) {
    this(
        owner,
        storage,
        security,
        captcha,
        registration,
        externalSearch,
        new Ai("https://api.deepseek.com/chat/completions", "", "deepseek-chat", 35),
        new WebImport("", "tesseract", 35, 30, 30_000, 20, false),
        Admin.disabled(),
        Billing.disabled());
  }

  public AppProperties(
      Owner owner,
      Storage storage,
      Security security,
      Captcha captcha,
      Registration registration,
      ExternalSearch externalSearch,
      Ai ai,
      WebImport webImport) {
    this(
        owner,
        storage,
        security,
        captcha,
        registration,
        externalSearch,
        ai,
        webImport,
        Admin.disabled(),
        Billing.disabled());
  }

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

  public record ExternalSearch(
      String searchEndpoint,
      String fetchEndpoint,
      String searchApiKey,
      String fetchApiKey,
      int timeoutSeconds,
      int maxResults,
      long candidateTtlSeconds) {}

  public record Ai(String endpoint, String apiKey, String model, int timeoutSeconds) {}

  public record WebImport(
      String workerJar,
      String tesseractCommand,
      int browserTimeoutSeconds,
      int ocrTimeoutSeconds,
      int maxTextChars,
      int maxItems,
      boolean allowUnbilledAi) {
    @ConstructorBinding
    public WebImport {}

    public WebImport(
        String workerJar,
        String tesseractCommand,
        int browserTimeoutSeconds,
        int ocrTimeoutSeconds,
        int maxTextChars,
        int maxItems) {
      this(
          workerJar,
          tesseractCommand,
          browserTimeoutSeconds,
          ocrTimeoutSeconds,
          maxTextChars,
          maxItems,
          false);
    }
  }

  public record Admin(String username, String password) {
    public static Admin disabled() {
      return new Admin("a115846", "");
    }
  }

  public record Billing(
      String environment,
      String databaseEnvironment,
      boolean enabled,
      String bridgeUrl,
      String appToBridgeSecret,
      String bridgeToAppSecret,
      String catalogVersion,
      String checkoutSuccessUrl,
      String webhookUrl,
      String refundSupportUrl,
      String refundResponseTarget,
      String adminEmails,
      long idempotencyTtlSeconds,
      long taskTtlSeconds,
      int trialPoints) {
    public static Billing disabled() {
      return new Billing(
          "test",
          "test",
          false,
          "http://127.0.0.1:8091",
          "",
          "",
          "2026-10-mvp",
          "http://127.0.0.1:4173/kb/billing/return",
          "http://127.0.0.1:8082/kb/api/internal/billing/waffo-events",
          "https://support.example.invalid/refund",
          "3 个工作日",
          "",
          86400,
          1800,
          100);
    }
  }
}

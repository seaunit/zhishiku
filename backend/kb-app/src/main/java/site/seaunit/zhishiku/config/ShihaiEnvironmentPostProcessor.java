package site.seaunit.zhishiku.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class ShihaiEnvironmentPostProcessor implements EnvironmentPostProcessor {
  private static final Map<String, String> MIGRATED_NAMES = migrationPairs();

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    Map<String, Object> resolvedAliases = new LinkedHashMap<>();
    MIGRATED_NAMES.forEach(
        (canonical, legacy) -> {
          String canonicalValue = environment.getProperty(canonical);
          String legacyValue = environment.getProperty(legacy);
          if (hasText(canonicalValue)
              && hasText(legacyValue)
              && !canonicalValue.equals(legacyValue)) {
            throw new IllegalStateException(
                "Conflicting Shihai environment variables: "
                    + canonical
                    + " and legacy "
                    + legacy
                    + " must have the same value during migration.");
          }
          String selectedValue =
              hasText(canonicalValue) ? canonicalValue : hasText(legacyValue) ? legacyValue : null;
          if (selectedValue != null) {
            resolvedAliases.put(canonical, selectedValue);
            resolvedAliases.put(legacy, selectedValue);
          }
        });
    if (!resolvedAliases.isEmpty()) {
      environment
          .getPropertySources()
          .addFirst(new MapPropertySource("shihaiEnvironmentAliases", resolvedAliases));
    }
  }

  public static Map<String, String> migrationPairs() {
    Map<String, String> pairs = new LinkedHashMap<>();
    pairs.put("SHIHAI_SERVER_PORT", "SERVER_PORT");
    pairs.put("SHIHAI_DATABASE_URL", "DATABASE_URL");
    pairs.put("SHIHAI_DATABASE_USERNAME", "DATABASE_USERNAME");
    pairs.put("SHIHAI_DATABASE_PASSWORD", "DATABASE_PASSWORD");
    pairs.put("SHIHAI_SESSION_COOKIE_SECURE", "SESSION_COOKIE_SECURE");
    pairs.put("SHIHAI_SMTP_HOST", "SMTP_HOST");
    pairs.put("SHIHAI_SMTP_PORT", "SMTP_PORT");
    pairs.put("SHIHAI_SMTP_USERNAME", "SMTP_USERNAME");
    pairs.put("SHIHAI_SMTP_PASSWORD", "SMTP_PASSWORD");
    pairs.put("SHIHAI_SMTP_SSL", "SMTP_SSL");
    pairs.put("SHIHAI_SMTP_FROM", "SMTP_FROM");
    pairs.put("SHIHAI_SMTP_FROM_NAME", "SMTP_FROM_NAME");
    pairs.put("SHIHAI_OWNER_EMAIL", "KB_OWNER_EMAIL");
    pairs.put("SHIHAI_OWNER_PASSWORD", "KB_OWNER_PASSWORD");
    pairs.put("SHIHAI_BOOTSTRAP_TOKEN", "KB_BOOTSTRAP_TOKEN");
    pairs.put("SHIHAI_AUTO_BOOTSTRAP", "KB_AUTO_BOOTSTRAP");
    pairs.put("SHIHAI_STORAGE_ROOT", "KB_STORAGE_ROOT");
    pairs.put("SHIHAI_CAPTCHA_TTL_SECONDS", "KB_CAPTCHA_TTL_SECONDS");
    pairs.put("SHIHAI_CAPTCHA_MAX_ENTRIES", "KB_CAPTCHA_MAX_ENTRIES");
    pairs.put("SHIHAI_CAPTCHA_EXPOSE_ANSWER", "KB_CAPTCHA_EXPOSE_ANSWER");
    pairs.put("SHIHAI_REGISTRATION_ENABLED", "REGISTRATION_ENABLED");
    pairs.put("SHIHAI_REGISTRATION_CODE_TTL_SECONDS", "REGISTRATION_CODE_TTL_SECONDS");
    pairs.put(
        "SHIHAI_REGISTRATION_RESEND_COOLDOWN_SECONDS", "REGISTRATION_RESEND_COOLDOWN_SECONDS");
    pairs.put("SHIHAI_REGISTRATION_SEND_WINDOW_SECONDS", "REGISTRATION_SEND_WINDOW_SECONDS");
    pairs.put("SHIHAI_REGISTRATION_MAX_SENDS_PER_EMAIL", "REGISTRATION_MAX_SENDS_PER_EMAIL");
    pairs.put("SHIHAI_REGISTRATION_MAX_SENDS_PER_IP", "REGISTRATION_MAX_SENDS_PER_IP");
    pairs.put("SHIHAI_REGISTRATION_MAX_ATTEMPTS", "REGISTRATION_MAX_ATTEMPTS");
    pairs.put("SHIHAI_REGISTRATION_MAX_CACHED_CODES", "REGISTRATION_MAX_CACHED_CODES");
    pairs.put("SHIHAI_REGISTRATION_EXPOSE_CODE", "REGISTRATION_EXPOSE_CODE");
    pairs.put("SHIHAI_DEEPSEEK_SEARCH_ENDPOINT", "DEEPSEEK_SEARCH_ENDPOINT");
    pairs.put("SHIHAI_DEEPSEEK_FETCH_ENDPOINT", "DEEPSEEK_FETCH_ENDPOINT");
    pairs.put("SHIHAI_DEEPSEEK_SEARCH_API_KEY", "DEEPSEEK_SEARCH_API_KEY");
    pairs.put("SHIHAI_DEEPSEEK_FETCH_API_KEY", "DEEPSEEK_FETCH_API_KEY");
    pairs.put("SHIHAI_DEEPSEEK_SEARCH_TIMEOUT_SECONDS", "DEEPSEEK_SEARCH_TIMEOUT_SECONDS");
    pairs.put("SHIHAI_DEEPSEEK_SEARCH_MAX_RESULTS", "DEEPSEEK_SEARCH_MAX_RESULTS");
    pairs.put(
        "SHIHAI_DEEPSEEK_SEARCH_CANDIDATE_TTL_SECONDS", "DEEPSEEK_SEARCH_CANDIDATE_TTL_SECONDS");
    pairs.put("SHIHAI_DEEPSEEK_AI_ENDPOINT", "DEEPSEEK_AI_ENDPOINT");
    pairs.put("SHIHAI_DEEPSEEK_API_KEY", "DEEPSEEK_API_KEY");
    pairs.put("SHIHAI_DEEPSEEK_MODEL", "DEEPSEEK_MODEL");
    pairs.put("SHIHAI_DEEPSEEK_AI_TIMEOUT_SECONDS", "DEEPSEEK_AI_TIMEOUT_SECONDS");
    pairs.put("SHIHAI_WEB_IMPORT_ALLOW_UNBILLED_AI", "KB_WEB_IMPORT_ALLOW_UNBILLED_AI");
    pairs.put("SHIHAI_BROWSER_WORKER_JAR", "KB_BROWSER_WORKER_JAR");
    pairs.put("SHIHAI_TESSERACT_COMMAND", "TESSERACT_COMMAND");
    pairs.put("SHIHAI_BROWSER_TIMEOUT_SECONDS", "KB_BROWSER_TIMEOUT_SECONDS");
    pairs.put("SHIHAI_OCR_TIMEOUT_SECONDS", "KB_OCR_TIMEOUT_SECONDS");
    pairs.put("SHIHAI_WEB_IMPORT_MAX_TEXT_CHARS", "KB_WEB_IMPORT_MAX_TEXT_CHARS");
    pairs.put("SHIHAI_WEB_IMPORT_MAX_ITEMS", "KB_WEB_IMPORT_MAX_ITEMS");
    pairs.put("SHIHAI_DOCUMENT_IMPORT_MAX_TEXT_CHARS", "KB_DOCUMENT_IMPORT_MAX_TEXT_CHARS");
    pairs.put("SHIHAI_DOCUMENT_IMPORT_TIMEOUT_SECONDS", "KB_DOCUMENT_IMPORT_TIMEOUT_SECONDS");
    pairs.put("SHIHAI_SCREENSHOT_IMPORT_MAX_TEXT_CHARS", "KB_SCREENSHOT_IMPORT_MAX_TEXT_CHARS");
    pairs.put("SHIHAI_APP_ENV", "APP_ENV");
    pairs.put("SHIHAI_BILLING_DB_ENV", "BILLING_DB_ENV");
    pairs.put("SHIHAI_BILLING_ENABLED", "BILLING_ENABLED");
    pairs.put("SHIHAI_BILLING_BRIDGE_URL", "BILLING_BRIDGE_URL");
    pairs.put("SHIHAI_APP_TO_BRIDGE_SECRET", "APP_TO_BRIDGE_SECRET");
    pairs.put("SHIHAI_BRIDGE_TO_APP_SECRET", "BRIDGE_TO_APP_SECRET");
    pairs.put("SHIHAI_BILLING_CATALOG_VERSION", "BILLING_CATALOG_VERSION");
    pairs.put("SHIHAI_CHECKOUT_SUCCESS_URL", "CHECKOUT_SUCCESS_URL");
    pairs.put("SHIHAI_BILLING_WEBHOOK_URL", "BILLING_WEBHOOK_URL");
    pairs.put("SHIHAI_REFUND_SUPPORT_URL", "REFUND_SUPPORT_URL");
    pairs.put("SHIHAI_REFUND_RESPONSE_TARGET", "REFUND_RESPONSE_TARGET");
    pairs.put("SHIHAI_BILLING_ADMIN_EMAILS", "BILLING_ADMIN_EMAILS");
    pairs.put("SHIHAI_BILLING_IDEMPOTENCY_TTL_SECONDS", "BILLING_IDEMPOTENCY_TTL_SECONDS");
    pairs.put("SHIHAI_BILLING_TASK_TTL_SECONDS", "BILLING_TASK_TTL_SECONDS");
    pairs.put("SHIHAI_BILLING_TRIAL_POINTS", "BILLING_TRIAL_POINTS");
    pairs.put("SHIHAI_WAFFO_ENV", "WAFFO_ENV");
    pairs.put("SHIHAI_WAFFO_STORE_ID", "WAFFO_STORE_ID");
    pairs.put("SHIHAI_WAFFO_MERCHANT_ID", "WAFFO_MERCHANT_ID");
    pairs.put("SHIHAI_WAFFO_PRIVATE_KEY_BASE64", "WAFFO_PRIVATE_KEY_BASE64");
    pairs.put("SHIHAI_WAFFO_PRODUCT_MAP", "WAFFO_PRODUCT_MAP");
    pairs.put("SHIHAI_WAFFO_CURRENCY", "WAFFO_CURRENCY");
    pairs.put("SHIHAI_WAFFO_DRY_RUN", "WAFFO_DRY_RUN");
    pairs.put("SHIHAI_WAFFO_BRIDGE_PORT", "WAFFO_BRIDGE_PORT");
    pairs.put("SHIHAI_WAFFO_BRIDGE_URL", "WAFFO_BRIDGE_URL");
    pairs.put("SHIHAI_WAFFO_SUCCESS_URL_ALLOWLIST", "WAFFO_SUCCESS_URL_ALLOWLIST");
    pairs.put("SHIHAI_INTERNAL_EVENT_URL", "INTERNAL_EVENT_URL");
    return Map.copyOf(pairs);
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}

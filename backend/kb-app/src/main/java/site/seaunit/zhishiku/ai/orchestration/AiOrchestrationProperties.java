package site.seaunit.zhishiku.ai.orchestration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai-orchestration")
public record AiOrchestrationProperties(
    String provider,
    String difyBaseUrl,
    String difyOrganizerApiKey,
    String difyChatApiKey,
    int timeoutSeconds,
    int maxRetries) {
  public AiOrchestrationProperties {
    provider = provider == null || provider.isBlank() ? "deepseek" : provider.strip().toLowerCase();
    if (!provider.equals("deepseek") && !provider.equals("dify")) {
      throw new IllegalArgumentException(
          "AI orchestration provider must be either deepseek or dify.");
    }
    difyBaseUrl =
        difyBaseUrl == null || difyBaseUrl.isBlank()
            ? "http://127.0.0.1:8501/v1"
            : difyBaseUrl.strip().replaceAll("/+$", "");
    if (!difyBaseUrl.startsWith("http://") && !difyBaseUrl.startsWith("https://")) {
      throw new IllegalArgumentException("Dify base URL must use HTTP or HTTPS.");
    }
    if (timeoutSeconds < 10 || timeoutSeconds > 180) {
      throw new IllegalArgumentException(
          "AI orchestration timeout must be between 10 and 180 seconds.");
    }
    if (maxRetries < 0 || maxRetries > 2) {
      throw new IllegalArgumentException("AI orchestration retries must be between 0 and 2.");
    }
  }

  public boolean usesDify() {
    return "dify".equals(provider);
  }
}

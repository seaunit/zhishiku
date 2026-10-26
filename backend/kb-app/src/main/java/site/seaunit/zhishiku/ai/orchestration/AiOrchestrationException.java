package site.seaunit.zhishiku.ai.orchestration;

public class AiOrchestrationException extends RuntimeException {
  private final String code;

  public AiOrchestrationException(String code, String message) {
    super(message);
    this.code = code;
  }

  public AiOrchestrationException(String code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public String code() {
    return code;
  }
}

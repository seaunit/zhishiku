package site.seaunit.zhishiku.billing;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class BillingException extends RuntimeException {
  private final HttpStatus status;
  private final String code;
  private final Map<String, Object> details;

  public BillingException(HttpStatus status, String code, String message) {
    this(status, code, message, Map.of());
  }

  public BillingException(
      HttpStatus status, String code, String message, Map<String, Object> details) {
    super(message);
    this.status = status;
    this.code = code;
    this.details = Map.copyOf(details);
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getCode() {
    return code;
  }

  public Map<String, Object> getDetails() {
    return details;
  }
}

package site.seaunit.zhishiku.auth.registration;

public class EmailDeliveryUnavailableException extends RuntimeException {
  public EmailDeliveryUnavailableException() {
    super("Email delivery is temporarily unavailable.");
  }

  public EmailDeliveryUnavailableException(Throwable cause) {
    super("Email delivery is temporarily unavailable.", cause);
  }
}

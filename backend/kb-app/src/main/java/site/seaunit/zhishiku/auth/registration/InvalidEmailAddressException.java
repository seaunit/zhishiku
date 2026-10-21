package site.seaunit.zhishiku.auth.registration;

public class InvalidEmailAddressException extends RuntimeException {
  public InvalidEmailAddressException() {
    super("Email address is invalid.");
  }
}

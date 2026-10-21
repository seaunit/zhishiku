package site.seaunit.zhishiku.auth.registration;

public class EmailCodeInvalidException extends RuntimeException {
  public EmailCodeInvalidException() {
    super("Email verification code is invalid or expired.");
  }
}

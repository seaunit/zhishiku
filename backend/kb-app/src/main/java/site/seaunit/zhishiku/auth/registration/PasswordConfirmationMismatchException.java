package site.seaunit.zhishiku.auth.registration;

public class PasswordConfirmationMismatchException extends RuntimeException {
  public PasswordConfirmationMismatchException() {
    super("Password confirmation does not match.");
  }
}

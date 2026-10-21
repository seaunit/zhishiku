package site.seaunit.zhishiku.auth.registration;

public class RegistrationClosedException extends RuntimeException {
  public RegistrationClosedException() {
    super("Registration is closed because the owner is already initialized.");
  }
}

package site.seaunit.zhishiku.auth.registration;

public class LocalRegistrationEmailSender implements RegistrationEmailSender {
  @Override
  public void send(String email, String code) {
    // Local tests receive the code through the explicit debug response.
  }
}

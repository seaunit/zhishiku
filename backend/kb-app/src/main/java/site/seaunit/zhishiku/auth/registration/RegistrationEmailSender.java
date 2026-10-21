package site.seaunit.zhishiku.auth.registration;

public interface RegistrationEmailSender {
  void send(String email, String code);
}

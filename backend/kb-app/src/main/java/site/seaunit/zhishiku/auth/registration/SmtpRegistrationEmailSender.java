package site.seaunit.zhishiku.auth.registration;

import jakarta.mail.MessagingException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

public class SmtpRegistrationEmailSender implements RegistrationEmailSender {
  private final JavaMailSender mailSender;
  private final String fromAddress;
  private final String fromName;

  public SmtpRegistrationEmailSender(
      JavaMailSender mailSender, String fromAddress, String fromName) {
    this.mailSender = mailSender;
    this.fromAddress = fromAddress;
    this.fromName = fromName;
  }

  @Override
  public void send(String email, String code) {
    try {
      var message = mailSender.createMimeMessage();
      var helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(fromAddress);
      helper.setTo(email);
      helper.setSubject("【" + fromName + "】注册邮箱验证码");
      helper.setText(
          RegistrationEmailTemplate.text(code, fromName),
          RegistrationEmailTemplate.html(code, fromName));
      mailSender.send(message);
    } catch (MailException | MessagingException error) {
      throw new EmailDeliveryUnavailableException(error);
    }
  }
}

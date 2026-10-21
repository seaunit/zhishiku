package site.seaunit.zhishiku.auth.registration;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

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
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromAddress);
    message.setTo(email);
    message.setSubject("【" + fromName + "】注册邮箱验证码");
    message.setText(
        """
        你正在注册%s。

        邮箱验证码：%s

        验证码 10 分钟内有效，只能使用一次。
        如果不是你本人操作，请忽略这封邮件。
        """
            .formatted(fromName, code));
    try {
      mailSender.send(message);
    } catch (MailException error) {
      throw new EmailDeliveryUnavailableException(error);
    }
  }
}

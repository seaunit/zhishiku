package site.seaunit.zhishiku.auth.registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class SmtpRegistrationEmailSenderTest {
  @Mock private JavaMailSender mailSender;

  @Test
  void sendsTheVerificationCodeToTheRequestedAddress() throws Exception {
    MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    SmtpRegistrationEmailSender sender =
        new SmtpRegistrationEmailSender(mailSender, "no-reply@mail.seaunit.site", "识海");

    sender.send("owner@example.com", "123456");

    verify(mailSender).send(mimeMessage);
    mimeMessage.saveChanges();
    assertThat(mimeMessage.getSubject()).contains("识海");
    assertThat(mimeMessage.getAllRecipients()[0].toString()).isEqualTo("owner@example.com");
    assertThat(mimeMessage.getContentType()).startsWith("multipart/");
    String content = collectContent(mimeMessage);
    assertThat(content).contains("邮箱验证码：123456").contains("<html");
  }

  private static String collectContent(Part part) throws Exception {
    Object content = part.getContent();
    if (content instanceof Multipart multipart) {
      StringBuilder result = new StringBuilder();
      for (int index = 0; index < multipart.getCount(); index++) {
        result.append(collectContent(multipart.getBodyPart(index)));
      }
      return result.toString();
    }
    return content == null ? "" : content.toString();
  }
}

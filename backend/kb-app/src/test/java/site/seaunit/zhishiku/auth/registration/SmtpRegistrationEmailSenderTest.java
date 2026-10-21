package site.seaunit.zhishiku.auth.registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class SmtpRegistrationEmailSenderTest {
  @Mock private JavaMailSender mailSender;

  @Test
  void sendsTheVerificationCodeToTheRequestedAddress() {
    SmtpRegistrationEmailSender sender =
        new SmtpRegistrationEmailSender(mailSender, "no-reply@mail.seaunit.site", "识海");

    sender.send("owner@example.com", "123456");

    ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(message.capture());
    assertThat(message.getValue().getFrom()).isEqualTo("no-reply@mail.seaunit.site");
    assertThat(message.getValue().getTo()).containsExactly("owner@example.com");
    assertThat(message.getValue().getSubject()).contains("识海");
    assertThat(message.getValue().getText()).contains("123456").contains("10 分钟");
  }
}

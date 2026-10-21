package site.seaunit.zhishiku.auth.registration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RegistrationEmailTemplateTest {
  @Test
  void rendersTheSystemStyledHtmlEmail() {
    String html = RegistrationEmailTemplate.html("123456", "识海");

    assertThat(html)
        .contains("完成识海注册")
        .contains("#0b3144")
        .contains("1")
        .contains("2")
        .contains("3")
        .contains("4")
        .contains("5")
        .contains("6")
        .contains("10 分钟")
        .contains("只能使用一次")
        .contains("系统邮件，请勿直接回复");
  }

  @Test
  void providesAPlainTextFallback() {
    String text = RegistrationEmailTemplate.text("123456", "识海");

    assertThat(text).contains("邮箱验证码：123456").contains("10 分钟内有效").contains("只能使用一次");
  }
}

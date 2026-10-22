package site.seaunit.zhishiku.webimport;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiTextSecretRedactorTest {
  private final AiTextSecretRedactor redactor = new AiTextSecretRedactor();

  @Test
  void removesCommonApiKeysAndCredentialAssignments() {
    AiTextSecretRedactor.RedactionResult result =
        redactor.redact(
            "正文 AUTH_TOKEN=auth-token-value client_secret=client-secret-value "
                + "password=very-secret-value "
                + "登录密码：abcd1234 访问令牌：token-value-123456 私钥：secret-value-abcdef "
                + "Authorization: Basic dXNlcjpwYXNzd29yZA== "
                + "Bearer abcdefghijklmnopqrstuvwxyz123456 "
                + "sk-test-1234567890abcdefghijklmnop");

    assertThat(result.applied()).isTrue();
    assertThat(result.text()).doesNotContain("auth-token-value");
    assertThat(result.text()).doesNotContain("client-secret-value");
    assertThat(result.text()).doesNotContain("very-secret-value");
    assertThat(result.text()).doesNotContain("abcd1234");
    assertThat(result.text()).doesNotContain("token-value-123456");
    assertThat(result.text()).doesNotContain("secret-value-abcdef");
    assertThat(result.text()).doesNotContain("dXNlcjpwYXNzd29yZA==");
    assertThat(result.text()).doesNotContain("abcdefghijklmnopqrstuvwxyz123456");
    assertThat(result.text()).doesNotContain("sk-test-1234567890abcdefghijklmnop");
    assertThat(result.text()).contains("[REDACTED]");
  }

  @Test
  void leavesOrdinaryKnowledgeTextUnchanged() {
    AiTextSecretRedactor.RedactionResult result = redactor.redact("这是一段普通的网页正文，不包含访问凭据。");

    assertThat(result.applied()).isFalse();
    assertThat(result.text()).isEqualTo("这是一段普通的网页正文，不包含访问凭据。");
  }
}

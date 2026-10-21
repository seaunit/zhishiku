package site.seaunit.zhishiku.auth;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;
import site.seaunit.zhishiku.auth.captcha.CaptchaService;
import site.seaunit.zhishiku.auth.registration.EmailCodeIssued;
import site.seaunit.zhishiku.auth.registration.EmailDomainNotAllowedException;
import site.seaunit.zhishiku.auth.registration.PasswordConfirmationMismatchException;
import site.seaunit.zhishiku.auth.registration.RateLimitedException;
import site.seaunit.zhishiku.auth.registration.RegistrationService;
import site.seaunit.zhishiku.owner.OwnerService;
import site.seaunit.zhishiku.security.SecurityConfig;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerRegistrationTest {
  @Autowired private MockMvc mvc;

  @MockBean private AuthenticationManager authenticationManager;
  @MockBean private OwnerService ownerService;
  @MockBean private CaptchaService captchaService;
  @MockBean private RegistrationService registrationService;

  @Test
  void reportsWhetherRegistrationIsOpen() throws Exception {
    when(registrationService.isRegistrationOpen()).thenReturn(true);

    mvc.perform(get("/api/v1/auth/registration-status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.open").value(true));
  }

  @Test
  void requiresGraphicalCaptchaBeforeSendingEmailCode() throws Exception {
    when(captchaService.consume("captcha-1", "WRONG")).thenReturn(false);

    mvc.perform(
            post("/api/v1/auth/register/email-code")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "owner@example.com",
                      "captchaId": "captcha-1",
                      "captchaCode": "WRONG"
                    }
                    """))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("CAPTCHA_INVALID"));

    verifyNoInteractions(registrationService);
  }

  @Test
  void sendsEmailCodeWithoutExposingItInProduction() throws Exception {
    when(captchaService.consume("captcha-1", "ABCDE")).thenReturn(true);
    when(registrationService.issueEmailCode("owner@example.com", "127.0.0.1"))
        .thenReturn(new EmailCodeIssued(600, 60, "123456"));

    mvc.perform(
            post("/api/v1/auth/register/email-code")
                .with(csrf())
                .remoteAddress("127.0.0.1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "owner@example.com",
                      "captchaId": "captcha-1",
                      "captchaCode": "ABCDE"
                    }
                    """))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.expiresInSeconds").value(600))
        .andExpect(jsonPath("$.resendAfterSeconds").value(60))
        .andExpect(jsonPath("$.debugCode").doesNotExist());
  }

  @Test
  void rejectsDisposableEmailDomain() throws Exception {
    when(captchaService.consume("captcha-1", "ABCDE")).thenReturn(true);
    when(registrationService.issueEmailCode("daniel.parkerca61598b@mailinator.com", "127.0.0.1"))
        .thenThrow(new EmailDomainNotAllowedException());

    mvc.perform(
            post("/api/v1/auth/register/email-code")
                .with(csrf())
                .remoteAddress("127.0.0.1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "daniel.parkerca61598b@mailinator.com",
                      "captchaId": "captcha-1",
                      "captchaCode": "ABCDE"
                    }
                    """))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("EMAIL_DOMAIN_NOT_ALLOWED"));
  }

  @Test
  void rejectsMismatchedPasswordConfirmation() throws Exception {
    when(registrationService.register(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new PasswordConfirmationMismatchException());

    mvc.perform(
            post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "owner@example.com",
                      "password": "correct-horse-battery",
                      "confirmPassword": "different-password",
                      "emailCode": "123456",
                      "locale": "zh-CN"
                    }
                    """))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("PASSWORD_CONFIRMATION_MISMATCH"));
  }

  @Test
  void returnsRetryAfterWhenRegistrationEmailIsRateLimited() throws Exception {
    when(registrationService.register(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new RateLimitedException(42));

    mvc.perform(
            post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "owner@example.com",
                      "password": "correct-horse-battery",
                      "confirmPassword": "correct-horse-battery",
                      "emailCode": "123456",
                      "locale": "zh-CN"
                    }
                    """))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "42"))
        .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
  }
}

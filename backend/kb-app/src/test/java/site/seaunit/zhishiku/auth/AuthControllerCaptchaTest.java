package site.seaunit.zhishiku.auth;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import site.seaunit.zhishiku.auth.captcha.CaptchaChallenge;
import site.seaunit.zhishiku.auth.captcha.CaptchaService;
import site.seaunit.zhishiku.auth.registration.RegistrationService;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;
import site.seaunit.zhishiku.owner.OwnerService;
import site.seaunit.zhishiku.security.SecurityConfig;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerCaptchaTest {
  @Autowired private MockMvc mvc;

  @MockBean private AuthenticationManager authenticationManager;
  @MockBean private OwnerService ownerService;
  @MockBean private CaptchaService captchaService;
  @MockBean private RegistrationService registrationService;

  @Test
  void issuesCaptchaWithoutAuthentication() throws Exception {
    when(captchaService.issue())
        .thenReturn(new CaptchaChallenge("captcha-1", "data:image/png;base64,abc", 120, "ABCDE"));

    mvc.perform(get("/api/v1/auth/captcha"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.captchaId").value("captcha-1"))
        .andExpect(jsonPath("$.image").value("data:image/png;base64,abc"))
        .andExpect(jsonPath("$.expiresInSeconds").value(120))
        .andExpect(jsonPath("$.debugAnswer").doesNotExist());
  }

  @Test
  void rejectsInvalidCaptchaBeforePasswordAuthentication() throws Exception {
    when(captchaService.consume("captcha-1", "WRONG")).thenReturn(false);

    mvc.perform(
            post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "owner@seaunit.site",
                      "password": "correct-horse-battery",
                      "captchaId": "captcha-1",
                      "captchaCode": "WRONG"
                    }
                    """))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("CAPTCHA_INVALID"));

    verifyNoInteractions(authenticationManager);
  }

  @Test
  void acceptsTheAdministratorUsernameAfterCaptchaValidation() throws Exception {
    OwnerAccountEntity admin =
        OwnerAccountEntity.createAdmin("a115846", "a115846@admin.seaunit.local", "hash", "zh-CN");
    UsernamePasswordAuthenticationToken authenticated =
        UsernamePasswordAuthenticationToken.authenticated("a115846", "hash", java.util.List.of());
    when(captchaService.consume("captcha-1", "A1B2C")).thenReturn(true);
    when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any()))
        .thenReturn(authenticated);
    when(ownerService.findByLoginName("a115846")).thenReturn(java.util.Optional.of(admin));

    mvc.perform(
            post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "a115846",
                      "password": "password",
                      "captchaId": "captcha-1",
                      "captchaCode": "A1B2C"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loginName").value("a115846"))
        .andExpect(jsonPath("$.admin").value(true));
  }

  @Test
  void rejectsADisabledAccountAfterCaptchaValidation() throws Exception {
    when(captchaService.consume("captcha-1", "A1B2C")).thenReturn(true);
    when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new DisabledException("Account is disabled."));

    mvc.perform(
            post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "disabled@example.com",
                      "password": "password",
                      "captchaId": "captcha-1",
                      "captchaCode": "A1B2C"
                    }
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
  }
}

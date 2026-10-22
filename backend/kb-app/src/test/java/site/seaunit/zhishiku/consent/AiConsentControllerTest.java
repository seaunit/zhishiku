package site.seaunit.zhishiku.consent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import site.seaunit.zhishiku.consent.AiConsentDtos.AiConsentResponse;
import site.seaunit.zhishiku.security.SecurityConfig;

@WebMvcTest(AiConsentController.class)
@Import({SecurityConfig.class, site.seaunit.zhishiku.common.ApiExceptionHandler.class})
class AiConsentControllerTest {
  @Autowired private MockMvc mvc;

  @MockBean private AiConsentService service;
  @MockBean private AuthenticationManager authenticationManager;

  @Test
  void requiresAuthentication() throws Exception {
    mvc.perform(get("/api/v1/ai-consents")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void returnsAllDefinedScopes() throws Exception {
    when(service.list(any()))
        .thenReturn(
            List.of(
                new AiConsentResponse(
                    "text",
                    "deepseek:chat",
                    "text",
                    true,
                    "ai-consent-v1",
                    Instant.parse("2026-10-22T00:00:00Z"))));

    mvc.perform(get("/api/v1/ai-consents"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].scope").value("text"))
        .andExpect(jsonPath("$[0].granted").value(true));
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void updatesConsentWithCsrf() throws Exception {
    when(service.update(any(), eq("text"), any()))
        .thenReturn(
            new AiConsentResponse(
                "text",
                "deepseek:chat",
                "text",
                false,
                "ai-consent-v1",
                Instant.parse("2026-10-22T00:00:00Z")));

    mvc.perform(
            put("/api/v1/ai-consents/{scope}", "text")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"granted\":false,\"consentVersion\":\"ai-consent-v1\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.granted").value(false));

    verify(service).update(any(), eq("text"), any());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void rejectsConsentUpdateWithoutCsrf() throws Exception {
    mvc.perform(
            put("/api/v1/ai-consents/{scope}", "text")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"granted\":true,\"consentVersion\":\"ai-consent-v1\"}"))
        .andExpect(status().isForbidden());
  }
}

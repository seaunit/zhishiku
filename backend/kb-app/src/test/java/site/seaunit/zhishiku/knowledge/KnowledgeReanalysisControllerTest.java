package site.seaunit.zhishiku.knowledge;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import site.seaunit.zhishiku.security.SecurityConfig;

@WebMvcTest(KnowledgeReanalysisController.class)
@Import({SecurityConfig.class, site.seaunit.zhishiku.common.ApiExceptionHandler.class})
class KnowledgeReanalysisControllerTest {
  private static final UUID ITEM_ID = UUID.fromString("123e4567-e89b-42d3-a456-426614174801");
  private static final UUID TASK_ID = UUID.fromString("123e4567-e89b-42d3-a456-426614174802");

  @Autowired private MockMvc mvc;

  @MockBean private KnowledgeReanalysisService service;
  @MockBean private AuthenticationManager authenticationManager;

  @Test
  void requiresAuthentication() throws Exception {
    mvc.perform(
            post("/api/v1/knowledge-items/{itemId}/reanalyze", ITEM_ID)
                .with(csrf())
                .header("Idempotency-Key", "reanalyze-controller")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"externalAiConsent\":true,\"billingTaskId\":null}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void reanalyzesWithReservedBillingTaskAndIdempotency() throws Exception {
    mvc.perform(
            post("/api/v1/knowledge-items/{itemId}/reanalyze", ITEM_ID)
                .with(csrf())
                .header("Idempotency-Key", "reanalyze-controller")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "externalAiConsent": true,
                      "billingTaskId": "123e4567-e89b-42d3-a456-426614174802"
                    }
                    """))
        .andExpect(status().isOk());

    verify(service)
        .reanalyze(
            eq(ITEM_ID),
            org.mockito.ArgumentMatchers.argThat(
                request ->
                    request != null
                        && request.externalAiConsent()
                        && TASK_ID.equals(request.billingTaskId())),
            eq("reanalyze-controller"),
            any());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void requiresCsrf() throws Exception {
    mvc.perform(
            post("/api/v1/knowledge-items/{itemId}/reanalyze", ITEM_ID)
                .header("Idempotency-Key", "reanalyze-controller")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"externalAiConsent\":true,\"billingTaskId\":null}"))
        .andExpect(status().isForbidden());
  }
}

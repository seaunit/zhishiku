package site.seaunit.zhishiku.webimport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
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
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.SourceSummary;
import site.seaunit.zhishiku.security.SecurityConfig;

@WebMvcTest(WebPageImportController.class)
@Import({SecurityConfig.class, site.seaunit.zhishiku.common.ApiExceptionHandler.class})
class WebPageImportControllerTest {
  private static final UUID BILLING_TASK_ID =
      UUID.fromString("123e4567-e89b-42d3-a456-426614174010");

  @Autowired private MockMvc mvc;

  @MockBean private WebPageImportService service;
  @MockBean private AuthenticationManager authenticationManager;

  @Test
  void rejectsUnauthenticatedImports() throws Exception {
    mvc.perform(
            post("/api/v1/web-imports")
                .with(csrf())
                .header("Idempotency-Key", "web-controller")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://example.com/article\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void requiresCsrfForImports() throws Exception {
    mvc.perform(
            post("/api/v1/web-imports")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://example.com/article\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void importsOnlyTheSubmittedUrlWithCsrf() throws Exception {
    when(service.importPage(
            argThat(url -> "https://example.com/article".equals(url)),
            eq(true),
            eq(BILLING_TASK_ID),
            eq("web-controller"),
            argThat(auth -> auth != null && "alice@example.com".equals(auth.getName()))))
        .thenReturn(
            new WebImportDtos.WebPageImportResponse(
                List.of(
                    new KnowledgeItemResponse(
                        UUID.fromString("123e4567-e89b-42d3-a456-426614174101"),
                        "整理后的知识",
                        "页面正文",
                        null,
                        1,
                        "completed",
                        "AI 摘要",
                        null,
                        "succeeded",
                        "active",
                        false,
                        new SourceSummary(
                            "webpage",
                            "page_capture",
                            "https://example.com/article",
                            Instant.parse("2026-10-22T00:00:00Z")),
                        null,
                        List.of(),
                        1,
                        Instant.parse("2026-10-22T00:00:00Z"),
                        Instant.parse("2026-10-22T00:00:00Z")))));

    mvc.perform(
            post("/api/v1/web-imports")
                .with(csrf())
                .header("Idempotency-Key", "web-controller")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "url": "https://example.com/article",
                      "externalAiConsent": true,
                      "billingTaskId": "123e4567-e89b-42d3-a456-426614174010"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].source.kind").value("webpage"))
        .andExpect(jsonPath("$.items[0].body").value("页面正文"));

    verify(service).importPage(any(), eq(true), eq(BILLING_TASK_ID), eq("web-controller"), any());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void requiresExplicitExternalAiConsent() throws Exception {
    when(service.importPage(any(), eq(false), isNull(), eq("web-consent"), any()))
        .thenThrow(
            new WebImportConsentRequiredException(
                "Explicit consent is required before sending page text to external AI."));

    mvc.perform(
            post("/api/v1/web-imports")
                .with(csrf())
                .header("Idempotency-Key", "web-consent")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://example.com/article\",\"externalAiConsent\":false}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("AI_EXTERNAL_CONSENT_REQUIRED"));
  }
}

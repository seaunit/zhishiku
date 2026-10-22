package site.seaunit.zhishiku.screenshotimport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.SourceSummary;
import site.seaunit.zhishiku.screenshotimport.ScreenshotImportDtos.ScreenshotImportResponse;
import site.seaunit.zhishiku.security.SecurityConfig;
import site.seaunit.zhishiku.webimport.WebImportConsentRequiredException;

@WebMvcTest(ScreenshotImportController.class)
@Import({SecurityConfig.class, site.seaunit.zhishiku.common.ApiExceptionHandler.class})
class ScreenshotImportControllerTest {
  private static final UUID BILLING_TASK_ID =
      UUID.fromString("123e4567-e89b-42d3-a456-426614174012");

  @Autowired private MockMvc mvc;

  @MockBean private ScreenshotImportService service;
  @MockBean private AuthenticationManager authenticationManager;

  @Test
  void rejectsUnauthenticatedScreenshotImports() throws Exception {
    mvc.perform(
            multipart("/api/v1/screenshot-imports")
                .file(file())
                .param("externalAiConsent", "true")
                .with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void requiresCsrfForScreenshotImports() throws Exception {
    mvc.perform(
            multipart("/api/v1/screenshot-imports").file(file()).param("externalAiConsent", "true"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void importsScreenshotAndReturnsCreatedKnowledge() throws Exception {
    when(service.importScreenshot(
            any(),
            eq("截图备注"),
            eq(true),
            eq(BILLING_TASK_ID),
            eq("shot-controller"),
            argThat(auth -> auth != null && "alice@example.com".equals(auth.getName()))))
        .thenReturn(new ScreenshotImportResponse(List.of(response())));

    mvc.perform(
            multipart("/api/v1/screenshot-imports")
                .file(file())
                .param("externalAiConsent", "true")
                .param("note", "截图备注")
                .param("billingTaskId", BILLING_TASK_ID.toString())
                .header("Idempotency-Key", "shot-controller")
                .with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].source.kind").value("screenshot"))
        .andExpect(jsonPath("$.items[0].processingStatus").value("succeeded"));
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void reportsMissingExternalAiConsent() throws Exception {
    when(service.importScreenshot(any(), any(), eq(false), isNull(), eq("shot-consent"), any()))
        .thenThrow(
            new WebImportConsentRequiredException(
                "Explicit consent is required before sending screenshot text to external AI."));

    mvc.perform(
            multipart("/api/v1/screenshot-imports")
                .file(file())
                .param("externalAiConsent", "false")
                .header("Idempotency-Key", "shot-consent")
                .with(csrf()))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("AI_EXTERNAL_CONSENT_REQUIRED"));
  }

  private static MockMultipartFile file() {
    return new MockMultipartFile("file", "clipboard.png", "image/png", new byte[] {1, 2, 3});
  }

  private static KnowledgeItemResponse response() {
    Instant now = Instant.parse("2026-10-22T00:00:00Z");
    return new KnowledgeItemResponse(
        UUID.fromString("123e4567-e89b-42d3-a456-426614174501"),
        "整理后的截图知识",
        "截图正文",
        null,
        1,
        "completed",
        "来源格式：PNG 截图",
        null,
        "succeeded",
        "active",
        false,
        new SourceSummary("screenshot", "clipboard", null, now),
        null,
        List.of(),
        1,
        now,
        now);
  }
}

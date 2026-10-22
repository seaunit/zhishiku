package site.seaunit.zhishiku.documentimport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
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
import site.seaunit.zhishiku.documentimport.DocumentImportDtos.DocumentImportResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.SourceSummary;
import site.seaunit.zhishiku.security.SecurityConfig;
import site.seaunit.zhishiku.webimport.WebImportConsentRequiredException;

@WebMvcTest(DocumentImportController.class)
@Import({SecurityConfig.class, site.seaunit.zhishiku.common.ApiExceptionHandler.class})
class DocumentImportControllerTest {
  private static final UUID BILLING_TASK_ID =
      UUID.fromString("123e4567-e89b-42d3-a456-426614174011");

  @Autowired private MockMvc mvc;

  @MockBean private DocumentImportService service;
  @MockBean private AuthenticationManager authenticationManager;

  @Test
  void rejectsUnauthenticatedDocumentImports() throws Exception {
    mvc.perform(
            multipart("/api/v1/document-imports")
                .file(file())
                .param("externalAiConsent", "true")
                .with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void requiresCsrfForDocumentImports() throws Exception {
    mvc.perform(
            multipart("/api/v1/document-imports").file(file()).param("externalAiConsent", "true"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void importsDocumentAndReturnsCreatedKnowledge() throws Exception {
    when(service.importDocument(
            any(),
            eq("我的备注"),
            eq(true),
            eq(BILLING_TASK_ID),
            eq("document-controller"),
            argThat(auth -> auth != null && "alice@example.com".equals(auth.getName()))))
        .thenReturn(new DocumentImportResponse(List.of(response())));

    mvc.perform(
            multipart("/api/v1/document-imports")
                .file(file())
                .param("externalAiConsent", "true")
                .param("note", "我的备注")
                .param("billingTaskId", BILLING_TASK_ID.toString())
                .header("Idempotency-Key", "document-controller")
                .with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].source.kind").value("document"))
        .andExpect(jsonPath("$.items[0].processingStatus").value("succeeded"));

    verify(service)
        .importDocument(
            any(), eq("我的备注"), eq(true), eq(BILLING_TASK_ID), eq("document-controller"), any());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void reportsMissingExternalAiConsent() throws Exception {
    when(service.importDocument(
            any(), isNull(), eq(false), isNull(), eq("document-consent"), any()))
        .thenThrow(
            new WebImportConsentRequiredException(
                "Explicit consent is required before sending document text to external AI."));

    mvc.perform(
            multipart("/api/v1/document-imports")
                .file(file())
                .param("externalAiConsent", "false")
                .header("Idempotency-Key", "document-consent")
                .with(csrf()))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("AI_EXTERNAL_CONSENT_REQUIRED"));
  }

  private static MockMultipartFile file() {
    return new MockMultipartFile(
        "file", "example.txt", "text/plain", "extractable document text".getBytes());
  }

  private static KnowledgeItemResponse response() {
    Instant now = Instant.parse("2026-10-22T00:00:00Z");
    return new KnowledgeItemResponse(
        UUID.fromString("123e4567-e89b-42d3-a456-426614174401"),
        "整理后的文档知识",
        "文档正文",
        null,
        1,
        "completed",
        "来源格式：TXT",
        null,
        "succeeded",
        "active",
        false,
        new SourceSummary("document", "upload", null, now),
        null,
        List.of(),
        1,
        now,
        now);
  }
}

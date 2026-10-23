package site.seaunit.zhishiku.documentimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import site.seaunit.zhishiku.billing.BillingService;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.CreateKnowledgeRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeService;
import site.seaunit.zhishiku.webimport.AiTextSecretRedactor;
import site.seaunit.zhishiku.webimport.DocumentOrganizer;
import site.seaunit.zhishiku.webimport.ImportedKnowledgeMapper;
import site.seaunit.zhishiku.webimport.OrganizedKnowledge;
import site.seaunit.zhishiku.webimport.WebImportBillingRequiredException;
import site.seaunit.zhishiku.webimport.WebImportConsentRequiredException;
import site.seaunit.zhishiku.webimport.WebPageOrganizationException;

class DocumentImportServiceTest {
  private final DocumentTextExtractor extractor = mock(DocumentTextExtractor.class);
  private final DocumentOrganizer organizer = mock(DocumentOrganizer.class);
  private final KnowledgeService knowledgeService = mock(KnowledgeService.class);
  private final Authentication authentication = mock(Authentication.class);
  private final MultipartFile file = mock(MultipartFile.class);
  private final ImportedKnowledgeMapper mapper = new ImportedKnowledgeMapper(new ObjectMapper());
  private final BillingService billingService = mock(BillingService.class);
  private final DocumentImportService service =
      new DocumentImportService(
          extractor,
          organizer,
          new AiTextSecretRedactor(),
          true,
          knowledgeService,
          mapper,
          billingService,
          mock(site.seaunit.zhishiku.consent.AiConsentService.class),
          mock(site.seaunit.zhishiku.idempotency.IdempotencyService.class));

  @Test
  void parsesRedactsAndCreatesMultipleDocumentKnowledgeItems() {
    when(extractor.extract(file))
        .thenReturn(
            new ExtractedDocument(
                "完整文档正文。".repeat(30) + "\napi_key=sk-test-1234567890abcdefghijklmnop",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                500));
    when(organizer.organize(
            eq("document"),
            argThat(
                text ->
                    text.contains("[REDACTED]")
                        && !text.contains("sk-test-1234567890abcdefghijklmnop"))))
        .thenReturn(
            List.of(
                new OrganizedKnowledge("第一章", "第一章完整正文。", "摘要一", "工程", List.of("文档")),
                new OrganizedKnowledge("第二章", "第二章完整正文。", "摘要二", "产品", List.of("整理"))));
    when(knowledgeService.createOrganized(any(), any(), any()))
        .thenReturn(response("第一章"), response("第二章"));

    var result = service.importDocument(file, "我的上下文", true, authentication);

    assertThat(result.items()).hasSize(2);
    assertThat(result.items()).allMatch(KnowledgeItemResponse::redactionApplied);
    ArgumentCaptor<CreateKnowledgeRequest> requests =
        ArgumentCaptor.forClass(CreateKnowledgeRequest.class);
    verify(knowledgeService, org.mockito.Mockito.times(2))
        .createOrganized(requests.capture(), any(), any());
    assertThat(requests.getAllValues())
        .allSatisfy(
            request -> {
              assertThat(request.bodySource().kind()).isEqualTo("document");
              assertThat(request.bodySource().sourceUrl()).isNull();
              assertThat(request.note())
                  .contains("来源格式：DOCX")
                  .contains("我的上下文")
                  .doesNotContain("用户备注：来源格式")
                  .contains("安全处理：已移除疑似密钥");
            });
  }

  @Test
  void createsDocumentKnowledgeFromImageOcrWithoutAnAttachment() {
    when(extractor.extract(file))
        .thenReturn(new ExtractedDocument("图片识别正文。".repeat(30), "image/png", 300));
    when(organizer.organize(eq("document"), any()))
        .thenReturn(
            List.of(new OrganizedKnowledge("图片中的知识", "图片识别整理正文。", "图片摘要", "资料", List.of("图片"))));
    when(knowledgeService.createOrganized(any(), any(), any())).thenReturn(response("图片中的知识"));

    service.importDocument(file, "图片备注", true, authentication);

    ArgumentCaptor<CreateKnowledgeRequest> request =
        ArgumentCaptor.forClass(CreateKnowledgeRequest.class);
    verify(knowledgeService).createOrganized(request.capture(), any(), any());
    assertThat(request.getValue().bodySource().kind()).isEqualTo("document");
    assertThat(request.getValue().bodyDocument().toString())
        .doesNotContain("\"type\":\"image\"")
        .doesNotContain("\"assetId\"");
    assertThat(request.getValue().note()).contains("来源格式：PNG 图片").contains("图片备注");
  }

  @Test
  void refusesExternalAiWithoutConsent() {
    try {
      service.importDocument(file, "", false, authentication);
    } catch (WebImportConsentRequiredException expected) {
      assertThat(expected.getMessage()).contains("consent");
    }
    verify(extractor, never()).extract(any());
  }

  @Test
  void refusesUnbilledDocumentAi() {
    DocumentImportService unbilled =
        new DocumentImportService(
            extractor,
            organizer,
            new AiTextSecretRedactor(),
            false,
            knowledgeService,
            mapper,
            billingService,
            mock(site.seaunit.zhishiku.consent.AiConsentService.class),
            mock(site.seaunit.zhishiku.idempotency.IdempotencyService.class));

    try {
      unbilled.importDocument(file, "", true, authentication);
    } catch (WebImportBillingRequiredException expected) {
      assertThat(expected.getMessage()).contains("billing");
    }
    verify(extractor, never()).extract(any());
  }

  @Test
  void doesNotCreateKnowledgeWhenAiOrganizationFails() {
    when(extractor.extract(file))
        .thenReturn(new ExtractedDocument("完整文档正文。".repeat(30), "application/pdf", 500));
    when(organizer.organize(eq("document"), any()))
        .thenThrow(new WebPageOrganizationException("AI returned no usable knowledge."));

    try {
      service.importDocument(file, "", true, authentication);
    } catch (DocumentImportException expected) {
      assertThat(expected.getCode()).isEqualTo("DOCUMENT_ORGANIZATION_FAILED");
    }
    verify(knowledgeService, never()).createOrganized(any(), any(), any());
  }

  @Test
  void rejectsOversizedUserNoteBeforeReadingTheFile() {
    try {
      service.importDocument(file, "x".repeat(20_001), true, authentication);
    } catch (DocumentImportException expected) {
      assertThat(expected.getCode()).isEqualTo("DOCUMENT_NOTE_TOO_LONG");
    }
    verify(extractor, never()).extract(any());
  }

  private static KnowledgeItemResponse response(String title) {
    return new KnowledgeItemResponse(
        UUID.randomUUID(),
        title,
        title,
        null,
        1,
        "completed",
        "",
        null,
        "review",
        "active",
        false,
        null,
        null,
        List.of(),
        1,
        Instant.now(),
        Instant.now());
  }
}

package site.seaunit.zhishiku.webimport;

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
import site.seaunit.zhishiku.billing.BillingService;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.CreateKnowledgeRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeService;

class WebPageImportServiceTest {
  private final WebPageCaptureGateway capture = mock(WebPageCaptureGateway.class);
  private final WebPageOrganizer organizer = mock(WebPageOrganizer.class);
  private final AiTextSecretRedactor secretRedactor = new AiTextSecretRedactor();
  private final KnowledgeService knowledgeService = mock(KnowledgeService.class);
  private final Authentication authentication = mock(Authentication.class);
  private final ImportedKnowledgeMapper knowledgeMapper =
      new ImportedKnowledgeMapper(new ObjectMapper());
  private final BillingService billingService = mock(BillingService.class);
  private final WebPageImportService service =
      new WebPageImportService(
          capture,
          organizer,
          secretRedactor,
          true,
          knowledgeService,
          knowledgeMapper,
          billingService,
          mock(site.seaunit.zhishiku.consent.AiConsentService.class),
          mock(site.seaunit.zhishiku.idempotency.IdempotencyService.class));

  @Test
  void createsMultipleKnowledgeItemsAndNeverStoresTheUrlAsBody() {
    when(capture.capture("https://example.com/article"))
        .thenReturn(
            new CapturedWebPage(
                "https://example.com/article",
                "https://example.com/article/final",
                "第一段网页正文。".repeat(30)
                    + "\n\n第二段网页正文。".repeat(30)
                    + "\napi_key=sk-test-1234567890abcdefghijklmnop"));
    when(organizer.organize(
            eq("web page"),
            argThat(
                text ->
                    text.contains("[REDACTED]")
                        && !text.contains("sk-test-1234567890abcdefghijklmnop"))))
        .thenReturn(
            List.of(
                new OrganizedKnowledge("第一篇", "第一段网页正文。", "摘要一", "工程", List.of("网页")),
                new OrganizedKnowledge("第二篇", "第二段网页正文。", "摘要二", "产品", List.of("知识"))));
    when(knowledgeService.createOrganized(any(), any(), any()))
        .thenReturn(response("第一篇"), response("第二篇"));

    var result = service.importPage("https://example.com/article", true, authentication);

    assertThat(result.items()).hasSize(2);
    assertThat(result.items()).allMatch(KnowledgeItemResponse::redactionApplied);
    ArgumentCaptor<CreateKnowledgeRequest> requests =
        ArgumentCaptor.forClass(CreateKnowledgeRequest.class);
    verify(knowledgeService, org.mockito.Mockito.times(2))
        .createOrganized(requests.capture(), any(), any());
    assertThat(requests.getAllValues())
        .allSatisfy(
            request -> {
              assertThat(request.bodySource().kind()).isEqualTo("webpage");
              assertThat(request.bodySource().sourceUrl()).isEqualTo("https://example.com/article");
              assertThat(request.note()).contains("安全处理：已移除疑似密钥");
              assertThat(
                      request
                          .bodyDocument()
                          .path("content")
                          .path(0)
                          .path("content")
                          .path(0)
                          .path("text")
                          .asText())
                  .isNotEqualTo("https://example.com/article");
            });
  }

  @Test
  void doesNotCreateKnowledgeWhenOrganizationFails() {
    when(capture.capture("https://example.com/article"))
        .thenReturn(
            new CapturedWebPage(
                "https://example.com/article", "https://example.com/article", "可见正文".repeat(30)));
    when(organizer.organize(any(), any()))
        .thenThrow(new WebPageOrganizationException("AI returned no usable knowledge."));

    try {
      service.importPage("https://example.com/article", true, authentication);
    } catch (WebPageOrganizationException expected) {
      assertThat(expected.getMessage()).contains("usable knowledge");
    }
    verify(knowledgeService, never()).createOrganized(any(), any(), any());
  }

  @Test
  void refusesExternalAiWithoutExplicitConsent() {
    try {
      service.importPage("https://example.com/article", false, authentication);
    } catch (WebImportConsentRequiredException expected) {
      assertThat(expected.getMessage()).contains("consent");
    }
    verify(capture, never()).capture(any());
    verify(organizer, never()).organize(any(), any());
  }

  @Test
  void refusesUnbilledAiUntilBillingIsConnected() {
    WebPageImportService unbilled =
        new WebPageImportService(
            capture,
            organizer,
            secretRedactor,
            false,
            knowledgeService,
            knowledgeMapper,
            billingService,
            mock(site.seaunit.zhishiku.consent.AiConsentService.class),
            mock(site.seaunit.zhishiku.idempotency.IdempotencyService.class));

    try {
      unbilled.importPage("https://example.com/article", true, authentication);
    } catch (WebImportBillingRequiredException expected) {
      assertThat(expected.getMessage()).contains("billing");
    }
    verify(capture, never()).capture(any());
  }

  @Test
  void returnsStableInvalidUrlErrorWithoutSendingPageTextToAi() {
    when(capture.capture(any()))
        .thenThrow(new IllegalArgumentException("Source URL must target a public host."));

    try {
      service.importPage("http://127.0.0.1/", true, authentication);
    } catch (WebPageUrlException expected) {
      assertThat(expected.getMessage()).contains("public HTTP or HTTPS");
    }
    verify(organizer, never()).organize(any(), any());
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

package site.seaunit.zhishiku.webimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import site.seaunit.zhishiku.knowledge.DuplicateKnowledge;
import site.seaunit.zhishiku.knowledge.DuplicateKnowledgeException;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.CreateKnowledgeRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeDuplicateService;
import site.seaunit.zhishiku.knowledge.KnowledgeService;
import site.seaunit.zhishiku.owner.CurrentOwnerService;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;
import site.seaunit.zhishiku.richtext.image.ImportImageAssetService;
import site.seaunit.zhishiku.richtext.image.ImportImageCandidate;
import site.seaunit.zhishiku.richtext.image.ImportedImageAsset;

class WebPageImportServiceTest {
  private final WebPageCaptureGateway capture = mock(WebPageCaptureGateway.class);
  private final WebPageOrganizer organizer = mock(WebPageOrganizer.class);
  private final AiTextSecretRedactor secretRedactor = new AiTextSecretRedactor();
  private final KnowledgeService knowledgeService = mock(KnowledgeService.class);
  private final Authentication authentication = mock(Authentication.class);
  private final ImportedKnowledgeMapper knowledgeMapper =
      new ImportedKnowledgeMapper(new ObjectMapper());
  private final BillingService billingService = mock(BillingService.class);
  private final ImportImageAssetService imageAssets = mock(ImportImageAssetService.class);
  private final KnowledgeDuplicateService knowledgeDuplicates =
      mock(KnowledgeDuplicateService.class);
  private final CurrentOwnerService currentOwnerService = mock(CurrentOwnerService.class);
  private final OwnerAccountEntity owner =
      OwnerAccountEntity.create("owner@example.com", "hash", "zh-CN");
  private final WebPageImportService service = createService(true);

  private WebPageImportService createService(boolean allowUnbilledAi) {
    when(currentOwnerService.require(authentication)).thenReturn(owner);
    return new WebPageImportService(
        capture,
        organizer,
        secretRedactor,
        allowUnbilledAi,
        knowledgeService,
        knowledgeDuplicates,
        currentOwnerService,
        knowledgeMapper,
        imageAssets,
        billingService,
        mock(site.seaunit.zhishiku.consent.AiConsentService.class),
        mock(site.seaunit.zhishiku.idempotency.IdempotencyService.class));
  }

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
  void includesSafeWebPageFigureInTheFirstRichTextKnowledgeItem() {
    ImportImageCandidate candidate =
        new ImportImageCandidate(new byte[] {1, 2, 3}, "webpage.png", "image/png");
    ImportedImageAsset asset =
        new ImportedImageAsset(UUID.randomUUID(), "网页架构图", "网页来源图示", 1440, 1800, 0.8);
    when(capture.capture("https://example.com/article"))
        .thenReturn(
            new CapturedWebPage(
                "https://example.com/article",
                "https://example.com/article",
                "网页可见正文。".repeat(30),
                List.of(candidate)));
    when(imageAssets.prepare(any(), any(), any(), any(), eq(false))).thenReturn(List.of(asset));
    when(organizer.organize(eq("web page"), any()))
        .thenReturn(
            List.of(
                new OrganizedKnowledge("网页知识一", "第一段整理正文。", "摘要一", "工程", List.of("网页")),
                new OrganizedKnowledge("网页知识二", "第二段整理正文。", "摘要二", "产品", List.of("整理"))));
    when(knowledgeService.createOrganized(any(), any(), any()))
        .thenReturn(response("网页知识一"), response("网页知识二"));

    service.importPage("https://example.com/article", true, authentication);

    ArgumentCaptor<CreateKnowledgeRequest> requests =
        ArgumentCaptor.forClass(CreateKnowledgeRequest.class);
    verify(knowledgeService, org.mockito.Mockito.times(2))
        .createOrganized(requests.capture(), any(), any());
    assertThat(requests.getAllValues().get(0).bodyDocument().toString())
        .contains("\"type\":\"image\"")
        .contains(asset.uploadId().toString());
    assertThat(requests.getAllValues().get(1).bodyDocument().toString())
        .doesNotContain("\"type\":\"image\"");
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
  void blocksAnAlreadyImportedPageBeforeCaptureAndReleasesReservedPoints() {
    String url = "https://example.com/article?utm_source=duplicate";
    UUID billingTaskId = UUID.randomUUID();
    when(knowledgeDuplicates.findActiveBySourceUrl(owner.getId(), url))
        .thenReturn(List.of(new DuplicateKnowledge(UUID.randomUUID(), "Existing article")));

    assertThatThrownBy(
            () -> service.importPage(url, true, billingTaskId, null, authentication))
        .isInstanceOf(DuplicateKnowledgeException.class);

    verify(billingService)
        .releaseAfterFailure(
            authentication, billingTaskId, "Knowledge source is already imported");
    verify(capture, never()).capture(any());
    verify(organizer, never()).organize(any(), any());
  }

  @Test
  void refusesUnbilledAiUntilBillingIsConnected() {
    WebPageImportService unbilled = createService(false);

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

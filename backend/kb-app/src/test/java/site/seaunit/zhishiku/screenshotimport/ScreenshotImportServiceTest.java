package site.seaunit.zhishiku.screenshotimport;

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
import site.seaunit.zhishiku.richtext.image.ImportImageAssetService;
import site.seaunit.zhishiku.richtext.image.ImportImageCandidate;
import site.seaunit.zhishiku.richtext.image.ImportedImageAsset;
import site.seaunit.zhishiku.webimport.AiTextSecretRedactor;
import site.seaunit.zhishiku.webimport.ImportedKnowledgeMapper;
import site.seaunit.zhishiku.webimport.OrganizedKnowledge;
import site.seaunit.zhishiku.webimport.ScreenshotOrganizer;
import site.seaunit.zhishiku.webimport.WebImportBillingRequiredException;
import site.seaunit.zhishiku.webimport.WebImportConsentRequiredException;
import site.seaunit.zhishiku.webimport.WebPageOrganizationException;

class ScreenshotImportServiceTest {
  private final ScreenshotTextExtractor extractor = mock(ScreenshotTextExtractor.class);
  private final ScreenshotOrganizer organizer = mock(ScreenshotOrganizer.class);
  private final KnowledgeService knowledgeService = mock(KnowledgeService.class);
  private final Authentication authentication = mock(Authentication.class);
  private final MultipartFile file = mock(MultipartFile.class);
  private final ImportedKnowledgeMapper mapper = new ImportedKnowledgeMapper(new ObjectMapper());
  private final ImportImageAssetService imageAssets = mock(ImportImageAssetService.class);
  private final BillingService billingService = mock(BillingService.class);
  private final ScreenshotImportService service =
      new ScreenshotImportService(
          extractor,
          organizer,
          new AiTextSecretRedactor(),
          true,
          knowledgeService,
          mapper,
          imageAssets,
          billingService,
          mock(site.seaunit.zhishiku.consent.AiConsentService.class),
          mock(site.seaunit.zhishiku.idempotency.IdempotencyService.class));

  @Test
  void createsMultipleScreenshotKnowledgeItemsWithoutAnAttachment() {
    when(extractor.extract(file))
        .thenReturn(
            new ExtractedScreenshot(
                "截图正文".repeat(30) + "\nAUTH_TOKEN=test-auth-token-value",
                "image/png",
                "PNG 截图",
                200));
    when(organizer.organize(
            eq("screenshot"),
            argThat(
                text -> text.contains("[REDACTED]") && !text.contains("test-auth-token-value"))))
        .thenReturn(
            List.of(
                new OrganizedKnowledge("第一段", "第一段完整正文。", "摘要一", "工程", List.of("截图")),
                new OrganizedKnowledge("第二段", "第二段完整正文。", "摘要二", "产品", List.of("整理"))));
    when(knowledgeService.createOrganized(any(), any(), any()))
        .thenReturn(response("第一段"), response("第二段"));

    var result = service.importScreenshot(file, "截图背景", true, "shot-multi", authentication);

    assertThat(result.items()).hasSize(2);
    assertThat(result.items()).allMatch(KnowledgeItemResponse::redactionApplied);
    ArgumentCaptor<CreateKnowledgeRequest> requests =
        ArgumentCaptor.forClass(CreateKnowledgeRequest.class);
    verify(knowledgeService, org.mockito.Mockito.times(2))
        .createOrganized(requests.capture(), any(), any());
    assertThat(requests.getAllValues())
        .allSatisfy(
            request -> {
              assertThat(request.bodySource().kind()).isEqualTo("screenshot");
              assertThat(request.bodySource().sourceUrl()).isNull();
              assertThat(request.bodyDocument().toString())
                  .doesNotContain("\"type\":\"image\"")
                  .doesNotContain("\"assetId\"");
              assertThat(request.note())
                  .contains("来源格式：PNG 截图")
                  .contains("截图背景")
                  .contains("安全处理：已移除疑似密钥");
            });
  }

  @Test
  void includesSafeScreenshotFigureInTheFirstRichTextKnowledgeItem() {
    ImportImageCandidate candidate =
        new ImportImageCandidate(new byte[] {1, 2, 3}, "clipboard.png", "image/png");
    ImportedImageAsset asset =
        new ImportedImageAsset(UUID.randomUUID(), "流程图", "截图来源图示 · PNG 截图", 1440, 900, 1.6);
    when(extractor.extract(file))
        .thenReturn(
            new ExtractedScreenshot(
                "截图正文".repeat(30), "image/png", "PNG 截图", 200, List.of(candidate)));
    when(imageAssets.prepare(any(), any(), any(), any(), eq(true))).thenReturn(List.of(asset));
    when(organizer.organize(eq("screenshot"), any()))
        .thenReturn(
            List.of(
                new OrganizedKnowledge("截图知识一", "第一段整理正文。", "摘要一", "工程", List.of("截图")),
                new OrganizedKnowledge("截图知识二", "第二段整理正文。", "摘要二", "产品", List.of("整理"))));
    when(knowledgeService.createOrganized(any(), any(), any()))
        .thenReturn(response("截图知识一"), response("截图知识二"));

    service.importScreenshot(file, "截图背景", true, "shot-figure", authentication);

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
  void refusesExternalAiWithoutConsent() {
    try {
      service.importScreenshot(file, "", false, "shot-consent", authentication);
    } catch (WebImportConsentRequiredException expected) {
      assertThat(expected.getMessage()).contains("consent");
    }
    verify(extractor, never()).extract(any());
  }

  @Test
  void refusesUnbilledScreenshotAi() {
    ScreenshotImportService unbilled =
        new ScreenshotImportService(
            extractor,
            organizer,
            new AiTextSecretRedactor(),
            false,
            knowledgeService,
            mapper,
            imageAssets,
            billingService,
            mock(site.seaunit.zhishiku.consent.AiConsentService.class),
            mock(site.seaunit.zhishiku.idempotency.IdempotencyService.class));

    try {
      unbilled.importScreenshot(file, "", true, "shot-billing", authentication);
    } catch (WebImportBillingRequiredException expected) {
      assertThat(expected.getMessage()).contains("billing");
    }
    verify(extractor, never()).extract(any());
  }

  @Test
  void doesNotCreateKnowledgeWhenAiOrganizationFails() {
    when(extractor.extract(file))
        .thenReturn(new ExtractedScreenshot("截图正文".repeat(30), "image/png", "PNG 截图", 200));
    when(organizer.organize(eq("screenshot"), any()))
        .thenThrow(new WebPageOrganizationException("AI returned no usable knowledge."));

    try {
      service.importScreenshot(file, "", true, "shot-fail", authentication);
    } catch (ScreenshotImportException expected) {
      assertThat(expected.getCode()).isEqualTo("SCREENSHOT_ORGANIZATION_FAILED");
    }
    verify(knowledgeService, never()).createOrganized(any(), any(), any());
  }

  @Test
  void reusesTheSameResponseForTheSameIdempotencyKey() {
    when(extractor.extract(file))
        .thenReturn(new ExtractedScreenshot("截图正文".repeat(30), "image/png", "PNG 截图", 200));
    when(organizer.organize(eq("screenshot"), any()))
        .thenReturn(List.of(new OrganizedKnowledge("第一段", "第一段完整正文。", "摘要", "工程", List.of("截图"))));
    when(knowledgeService.createOrganized(any(), any(), any())).thenReturn(response("第一段"));

    var first = service.importScreenshot(file, "", true, "shot-idempotent", authentication);
    var second = service.importScreenshot(file, "", true, "shot-idempotent", authentication);

    assertThat(second).isSameAs(first);
    verify(extractor, org.mockito.Mockito.times(1)).extract(file);
  }

  @Test
  void requiresAValidIdempotencyKey() {
    try {
      service.importScreenshot(file, "", true, " ", authentication);
    } catch (ScreenshotImportException expected) {
      assertThat(expected.getCode()).isEqualTo("IDEMPOTENCY_KEY_REQUIRED");
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

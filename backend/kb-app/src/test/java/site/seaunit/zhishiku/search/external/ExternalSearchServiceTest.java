package site.seaunit.zhishiku.search.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import site.seaunit.zhishiku.billing.BillingService;
import site.seaunit.zhishiku.config.AppProperties;
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
import site.seaunit.zhishiku.search.external.ExternalSearchDtos.ExternalSearchRequest;
import site.seaunit.zhishiku.webimport.AiTextSecretRedactor;
import site.seaunit.zhishiku.webimport.CapturedWebPage;
import site.seaunit.zhishiku.webimport.ImportedKnowledgeMapper;
import site.seaunit.zhishiku.webimport.InternetResultOrganizer;
import site.seaunit.zhishiku.webimport.OrganizedKnowledge;
import site.seaunit.zhishiku.webimport.WebImportBillingRequiredException;
import site.seaunit.zhishiku.webimport.WebImportConsentRequiredException;
import site.seaunit.zhishiku.webimport.WebImportUnavailableException;
import site.seaunit.zhishiku.webimport.WebPageCaptureGateway;

@ExtendWith(MockitoExtension.class)
class ExternalSearchServiceTest {
  @Mock private DeepSeekWebProvider provider;
  @Mock private KnowledgeService knowledgeService;
  @Mock private KnowledgeDuplicateService knowledgeDuplicates;
  @Mock private InternetResultOrganizer organizer;
  @Mock private DeepSeekAnswerService answerService;
  @Mock private BillingService billingService;
  @Mock private CurrentOwnerService currentOwnerService;
  @Mock private WebPageCaptureGateway pageCapture;
  @Mock private ImportImageAssetService imageAssets;
  @Mock private Authentication authentication;

  private ExternalSearchService service;
  private OwnerAccountEntity owner;

  @BeforeEach
  void setUp() {
    owner = OwnerAccountEntity.create("owner@example.com", "hash", "zh-CN");
    Clock clock = Clock.fixed(Instant.parse("2026-10-22T00:00:00Z"), ZoneOffset.UTC);
    service =
        new ExternalSearchService(
            provider,
            knowledgeService,
            knowledgeDuplicates,
            organizer,
            answerService,
            new AiTextSecretRedactor(),
            new ImportedKnowledgeMapper(new ObjectMapper()),
            currentOwnerService,
            properties(),
            true,
            billingService,
            org.mockito.Mockito.mock(site.seaunit.zhishiku.consent.AiConsentService.class),
            org.mockito.Mockito.mock(site.seaunit.zhishiku.idempotency.IdempotencyService.class),
            pageCapture,
            imageAssets,
            clock);
  }

  @Test
  void cachesCandidatesByOwnerAndOrganizesFetchedContentBeforeSaving() {
    when(currentOwnerService.require(authentication)).thenReturn(owner);
    when(provider.search("DeepSeek", 5))
        .thenReturn(
            List.of(
                new DeepSeekWebProvider.SearchResult(
                    "DeepSeek",
                    "https://www.deepseek.com/",
                    "Official site",
                    "DeepSeek summary",
                    "DeepSeek",
                    null,
                    "2026-10-22T00:00:00Z",
                    4,
                    2.8,
                    "Official search-provided article content. ".repeat(20))));
    var search = service.search(new ExternalSearchRequest("DeepSeek", true, false), authentication);
    assertThat(search.items()).hasSize(1);

    when(provider.fetch("https://www.deepseek.com/"))
        .thenReturn(
            new DeepSeekWebProvider.FetchedPage(
                "DeepSeek",
                "# DeepSeek\n\nOfficial page content.\napi_key=sk-test-1234567890abcdefghijklmnop"));
    when(organizer.organize(
            eq("internet search result"),
            argThat(
                text ->
                    text.contains("[REDACTED]")
                        && !text.contains("sk-test-1234567890abcdefghijklmnop"))))
        .thenReturn(
            List.of(
                new OrganizedKnowledge(
                    "DeepSeek", "Official page knowledge.", "摘要", "资料", List.of("搜索"))));
    when(knowledgeService.createOrganized(any(), any(), any())).thenReturn(savedResponse());

    var saved = service.save(search.items().get(0).candidateId(), true, authentication);

    ArgumentCaptor<CreateKnowledgeRequest> request =
        ArgumentCaptor.forClass(CreateKnowledgeRequest.class);
    verify(knowledgeService).createOrganized(request.capture(), any(), any());
    assertThat(saved.items()).hasSize(1);
    assertThat(request.getValue().title()).isEqualTo("DeepSeek");
    assertThat(request.getValue().bodySource().kind()).isEqualTo("internet_result");
    assertThat(request.getValue().bodySource().sourceUrl()).isEqualTo("https://www.deepseek.com/");
    assertThat(request.getValue().bodyDocument().toString()).contains("Official page knowledge");
    assertThat(request.getValue().note()).contains("互联网搜索结果").contains("AI 分类：资料");
  }

  @Test
  void refusesSavingWithoutExplicitAiConsent() {
    try {
      service.save(java.util.UUID.randomUUID(), false, authentication);
    } catch (WebImportConsentRequiredException expected) {
      assertThat(expected.getMessage()).contains("consent");
    }
    verify(provider, never()).fetch(any());
    verify(organizer, never()).organize(any(), any());
  }

  @Test
  void keepsSearchResultsWithoutOrganizableContentButMarksThemUnavailable() {
    when(currentOwnerService.require(authentication)).thenReturn(owner);
    when(provider.search("empty", 5))
        .thenReturn(
            List.of(
                new DeepSeekWebProvider.SearchResult(
                    "Empty result",
                    "https://example.com/empty",
                    "No body",
                    "No body",
                    "Example",
                    null,
                    null,
                    1,
                    0.5,
                    "   ")));

    var search = service.search(new ExternalSearchRequest("empty", true, false), authentication);

    assertThat(search.items()).hasSize(1);
    assertThat(search.items().get(0).organizable()).isFalse();
  }

  @Test
  void marksCandidatesThatAreAlreadySavedForTheCurrentOwner() {
    when(currentOwnerService.require(authentication)).thenReturn(owner);
    when(provider.search("saved", 5))
        .thenReturn(
            List.of(
                new DeepSeekWebProvider.SearchResult(
                    "Saved result",
                    "https://example.com/saved?utm_source=test",
                    "Saved body",
                    "Saved summary",
                    "Example",
                    null,
                    null,
                    1,
                    0.8,
                    "Saved article content. ".repeat(20))));
    UUID existingId = UUID.randomUUID();
    when(knowledgeDuplicates.findActiveBySourceUrl(owner.getId(), "https://example.com/saved?utm_source=test"))
        .thenReturn(List.of(new DuplicateKnowledge(existingId, "Saved knowledge")));

    var search = service.search(new ExternalSearchRequest("saved", true, false), authentication);

    assertThat(search.items()).hasSize(1);
    assertThat(search.items().get(0).alreadyImported()).isTrue();
    assertThat(search.items().get(0).existingItemId()).isEqualTo(existingId);
    assertThat(search.items().get(0).existingItemTitle()).isEqualTo("Saved knowledge");
    assertThat(search.items().get(0).existingItemCount()).isEqualTo(1);
  }

  @Test
  void releasesReservedPointsWhenTheSourceWasImportedBeforeSave() {
    String url = "https://example.com/race";
    when(currentOwnerService.require(authentication)).thenReturn(owner);
    when(provider.search("race", 5))
        .thenReturn(
            List.of(
                new DeepSeekWebProvider.SearchResult(
                    "Race result",
                    url,
                    "Race body",
                    "Race summary",
                    "Example",
                    null,
                    null,
                    1,
                    0.8,
                    "Race article content. ".repeat(20))));
    var search = service.search(new ExternalSearchRequest("race", true, false), authentication);
    UUID billingTaskId = UUID.randomUUID();
    when(knowledgeDuplicates.findActiveBySourceUrl(owner.getId(), url))
        .thenReturn(List.of(new DuplicateKnowledge(UUID.randomUUID(), "Existing knowledge")));

    assertThatThrownBy(
            () ->
                service.save(
                    search.items().get(0).candidateId(), true, billingTaskId, authentication))
        .isInstanceOf(DuplicateKnowledgeException.class);

    verify(billingService)
        .releaseAfterFailure(
            authentication, billingTaskId, "Knowledge source is already imported");
    verify(provider, never()).fetch(url);
    verify(organizer, never()).organize(any(), any());
  }

  @Test
  void capturesPageImagesAndMapsInternetKnowledgeToRichText() {
    String url = "https://example.com/visual";
    String content = "Visual article content. ".repeat(20);
    when(currentOwnerService.require(authentication)).thenReturn(owner);
    when(provider.search("visual", 5))
        .thenReturn(
            List.of(
                new DeepSeekWebProvider.SearchResult(
                    "Visual article",
                    url,
                    "Visual article",
                    "Article summary",
                    "Example",
                    null,
                    "2026-10-22T00:00:00Z",
                    2,
                    1.8,
                    content)));
    var search = service.search(new ExternalSearchRequest("visual", true, false), authentication);
    ImportImageCandidate candidate =
        new ImportImageCandidate(new byte[] {1, 2, 3}, "page.png", "image/png");
    ImportedImageAsset asset =
        new ImportedImageAsset(
            java.util.UUID.randomUUID(), "互联网内容架构图", "互联网搜索结果图示", 1200, 800, 1.5);
    when(pageCapture.capture(url))
        .thenReturn(new CapturedWebPage(url, url, "网页截图可见文字。".repeat(30), List.of(candidate)));
    when(provider.fetch(url))
        .thenReturn(
            new DeepSeekWebProvider.FetchedPage(
                "Visual article", "## 核心结论\n\n**重点内容**\n\n- 第一项\n- 第二项"));
    when(organizer.organize(eq("internet search result"), any()))
        .thenReturn(
            List.of(
                new OrganizedKnowledge(
                    "图解互联网知识", "## 核心结论\n\n**重点内容**\n\n- 第一项\n- 第二项", "摘要", "资料", List.of("搜索"))));
    when(imageAssets.prepare(any(), eq(authentication), eq("图解互联网知识"), any(), eq(false)))
        .thenReturn(List.of(asset));
    when(knowledgeService.createOrganized(any(), any(), any())).thenReturn(savedResponse());

    java.util.UUID billingTaskId = java.util.UUID.randomUUID();
    service.save(search.items().get(0).candidateId(), true, billingTaskId, authentication);

    ArgumentCaptor<CreateKnowledgeRequest> request =
        ArgumentCaptor.forClass(CreateKnowledgeRequest.class);
    verify(knowledgeService).createOrganized(request.capture(), any(), any());
    assertThat(request.getValue().bodyDocument().toString())
        .contains("\"type\":\"heading\"")
        .contains("\"type\":\"bulletList\"")
        .contains("\"type\":\"image\"")
        .contains(asset.uploadId().toString());
    verify(billingService)
        .settle(
            authentication,
            billingTaskId,
            Set.of(
                site.seaunit.zhishiku.core.billing.BillingModel.Feature.WEBPAGE_VISION,
                site.seaunit.zhishiku.core.billing.BillingModel.Feature.CLASSIFY),
            "Internet result organized");
  }

  @Test
  void settlesOnlyClassificationWhenInternetPageCaptureFails() {
    String url = "https://example.com/fallback";
    String content = "Fallback article content. ".repeat(20);
    when(currentOwnerService.require(authentication)).thenReturn(owner);
    when(provider.search("fallback", 5))
        .thenReturn(
            List.of(
                new DeepSeekWebProvider.SearchResult(
                    "Fallback article",
                    url,
                    "Fallback article",
                    "Article summary",
                    "Example",
                    null,
                    "2026-10-22T00:00:00Z",
                    2,
                    1.8,
                    content)));
    var search = service.search(new ExternalSearchRequest("fallback", true, false), authentication);
    when(pageCapture.capture(url))
        .thenThrow(new WebImportUnavailableException("Page capture is unavailable."));
    when(provider.fetch(url))
        .thenReturn(
            new DeepSeekWebProvider.FetchedPage("Fallback article", "## 文本知识\n\n抓取正文仍可整理。"));
    when(organizer.organize(eq("internet search result"), any()))
        .thenReturn(
            List.of(
                new OrganizedKnowledge(
                    "Fallback knowledge", "## 文本知识\n\n抓取正文仍可整理。", "摘要", "资料", List.of("搜索"))));
    when(knowledgeService.createOrganized(any(), any(), any())).thenReturn(savedResponse());
    java.util.UUID billingTaskId = java.util.UUID.randomUUID();

    service.save(search.items().get(0).candidateId(), true, billingTaskId, authentication);

    verify(billingService)
        .settle(
            authentication,
            billingTaskId,
            Set.of(site.seaunit.zhishiku.core.billing.BillingModel.Feature.CLASSIFY),
            "Internet result organized");
  }

  @Test
  void rejectsDirectSaveOfAnUnorganizableCandidateWithoutCallingFetch() {
    String url = "https://example.com/empty";
    when(currentOwnerService.require(authentication)).thenReturn(owner);
    when(provider.search("empty", 5))
        .thenReturn(
            List.of(
                new DeepSeekWebProvider.SearchResult(
                    "Empty result",
                    url,
                    "No body",
                    "No body",
                    "Example",
                    null,
                    null,
                    1,
                    0.5,
                    "   ")));
    var search = service.search(new ExternalSearchRequest("empty", true, false), authentication);

    assertThatThrownBy(
            () -> service.save(search.items().get(0).candidateId(), true, authentication))
        .isInstanceOf(ExternalSourceUnavailableException.class);
    verify(provider, never()).fetch(url);
    verify(organizer, never()).organize(any(), any());
  }

  @Test
  void usesSearchProvidedContentWhenSingleSourceFetchFails() {
    String url = "https://eu.36kr.com/zh/p/4035009546000259";
    String content = "36Kr article body. ".repeat(20);
    when(currentOwnerService.require(authentication)).thenReturn(owner);
    when(provider.search("36Kr", 5))
        .thenReturn(
            List.of(
                new DeepSeekWebProvider.SearchResult(
                    "36Kr article",
                    url,
                    "Article snippet",
                    "Article summary",
                    "36Kr",
                    null,
                    "2026-10-22T00:00:00Z",
                    3,
                    1.5,
                    content)));
    var search = service.search(new ExternalSearchRequest("36Kr", true, false), authentication);
    when(provider.fetch(url))
        .thenThrow(
            new ExternalSourceUnavailableException("The selected source could not be fetched."));
    when(organizer.organize(
            eq("internet search result"), argThat(text -> text.contains("36Kr article body"))))
        .thenReturn(
            List.of(
                new OrganizedKnowledge(
                    "36Kr article", "Organized 36Kr knowledge.", "摘要", "资料", List.of("搜索"))));
    when(knowledgeService.createOrganized(any(), any(), any())).thenReturn(savedResponse());

    var saved = service.save(search.items().get(0).candidateId(), true, authentication);

    assertThat(saved.items()).hasSize(1);
  }

  @Test
  void refusesUnbilledInternetResultOrganization() {
    ExternalSearchService unbilled =
        new ExternalSearchService(
            provider,
            knowledgeService,
            knowledgeDuplicates,
            organizer,
            answerService,
            new AiTextSecretRedactor(),
            new ImportedKnowledgeMapper(new ObjectMapper()),
            currentOwnerService,
            properties(),
            false,
            billingService,
            org.mockito.Mockito.mock(site.seaunit.zhishiku.consent.AiConsentService.class),
            org.mockito.Mockito.mock(site.seaunit.zhishiku.idempotency.IdempotencyService.class),
            pageCapture,
            imageAssets,
            Clock.fixed(Instant.parse("2026-10-22T00:00:00Z"), ZoneOffset.UTC));
    try {
      unbilled.save(java.util.UUID.randomUUID(), true, authentication);
    } catch (WebImportBillingRequiredException expected) {
      assertThat(expected.getMessage()).contains("billing");
    }
    verify(provider, never()).fetch(any());
  }

  private static KnowledgeItemResponse savedResponse() {
    Instant now = Instant.parse("2026-10-22T00:00:00Z");
    return new KnowledgeItemResponse(
        java.util.UUID.randomUUID(),
        "DeepSeek",
        "Official page content.",
        null,
        1,
        "completed",
        "DeepSeek summary",
        null,
        "review",
        "active",
        false,
        null,
        null,
        List.of(),
        1,
        now,
        now);
  }

  private static AppProperties properties() {
    return new AppProperties(
        new AppProperties.Owner("", "", "", false),
        new AppProperties.Storage(".data/uploads"),
        new AppProperties.Security("zhishiku_session", "/kb"),
        new AppProperties.Captcha(120, 1000, false),
        new AppProperties.Registration(
            true, 600, 60, 3600, 5, 20, 5, 1000, false, "no-reply@example.com", "识海"),
        new AppProperties.ExternalSearch(
            "https://api.deepseek.com/v1/tools/search",
            "https://api.deepseek.com/v1/tools/fetch",
            "search-key",
            "fetch-key",
            20,
            5,
            900));
  }
}

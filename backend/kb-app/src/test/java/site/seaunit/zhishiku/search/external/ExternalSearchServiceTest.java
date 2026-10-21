package site.seaunit.zhishiku.search.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import site.seaunit.zhishiku.config.AppProperties;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.CreateKnowledgeRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeService;
import site.seaunit.zhishiku.owner.CurrentOwnerService;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;
import site.seaunit.zhishiku.richtext.migration.LegacyTextDocumentConverter;
import site.seaunit.zhishiku.search.external.ExternalSearchDtos.ExternalSearchRequest;

@ExtendWith(MockitoExtension.class)
class ExternalSearchServiceTest {
  @Mock private DeepSeekWebProvider provider;
  @Mock private KnowledgeService knowledgeService;
  @Mock private CurrentOwnerService currentOwnerService;
  @Mock private Authentication authentication;

  private ExternalSearchService service;
  private OwnerAccountEntity owner;

  @BeforeEach
  void setUp() {
    owner = OwnerAccountEntity.create("owner@example.com", "hash", "zh-CN");
    when(currentOwnerService.require(authentication)).thenReturn(owner);
    Clock clock = Clock.fixed(Instant.parse("2026-10-22T00:00:00Z"), ZoneOffset.UTC);
    service =
        new ExternalSearchService(
            provider,
            knowledgeService,
            new LegacyTextDocumentConverter(),
            currentOwnerService,
            properties(),
            clock);
  }

  @Test
  void cachesCandidatesByOwnerAndSavesFetchedContentAsInternetResult() {
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
                    2.8)));
    var search = service.search(new ExternalSearchRequest("DeepSeek", true, false), authentication);
    assertThat(search.items()).hasSize(1);

    when(provider.fetch("https://www.deepseek.com/"))
        .thenReturn(
            new DeepSeekWebProvider.FetchedPage(
                "DeepSeek", "# DeepSeek\n\nOfficial page content."));
    when(knowledgeService.create(any(), any())).thenReturn(savedResponse());

    service.save(search.items().get(0).candidateId(), authentication);

    ArgumentCaptor<CreateKnowledgeRequest> request =
        ArgumentCaptor.forClass(CreateKnowledgeRequest.class);
    verify(knowledgeService).create(request.capture(), any());
    assertThat(request.getValue().title()).isEqualTo("DeepSeek");
    assertThat(request.getValue().bodySource().kind()).isEqualTo("internet_result");
    assertThat(request.getValue().bodySource().sourceUrl()).isEqualTo("https://www.deepseek.com/");
    assertThat(request.getValue().bodyDocument().toString()).contains("Official page content");
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
        "not_requested",
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

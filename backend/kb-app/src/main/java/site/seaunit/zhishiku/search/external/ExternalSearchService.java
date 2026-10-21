package site.seaunit.zhishiku.search.external;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import site.seaunit.zhishiku.config.AppProperties;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.BodySourceRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.CreateKnowledgeRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeService;
import site.seaunit.zhishiku.owner.CurrentOwnerService;
import site.seaunit.zhishiku.richtext.migration.LegacyTextDocumentConverter;
import site.seaunit.zhishiku.search.external.ExternalSearchDtos.ExternalSearchCandidate;
import site.seaunit.zhishiku.search.external.ExternalSearchDtos.ExternalSearchRequest;
import site.seaunit.zhishiku.search.external.ExternalSearchDtos.ExternalSearchResponse;

@Service
public class ExternalSearchService {
  private static final int MAX_CONTENT_CHARS = 200_000;

  private final DeepSeekWebProvider provider;
  private final KnowledgeService knowledgeService;
  private final LegacyTextDocumentConverter textConverter;
  private final CurrentOwnerService currentOwnerService;
  private final AppProperties.ExternalSearch config;
  private final Clock clock;
  private final Cache<UUID, CachedCandidate> candidates;

  public ExternalSearchService(
      DeepSeekWebProvider provider,
      KnowledgeService knowledgeService,
      LegacyTextDocumentConverter textConverter,
      CurrentOwnerService currentOwnerService,
      AppProperties properties,
      Clock clock) {
    this.provider = provider;
    this.knowledgeService = knowledgeService;
    this.textConverter = textConverter;
    this.currentOwnerService = currentOwnerService;
    this.config = properties.externalSearch();
    this.clock = clock;
    this.candidates =
        Caffeine.newBuilder()
            .maximumSize(5_000)
            .expireAfterWrite(Duration.ofSeconds(Math.max(60, config.candidateTtlSeconds())))
            .build();
  }

  public ExternalSearchResponse search(
      ExternalSearchRequest request, Authentication authentication) {
    if (!request.explicitConsent()) {
      throw new IllegalArgumentException(
          "Explicit consent is required before sending a query to internet search.");
    }
    var owner = currentOwnerService.require(authentication);
    int count = Math.max(1, Math.min(10, config.maxResults()));
    Instant expiresAt = clock.instant().plusSeconds(Math.max(60, config.candidateTtlSeconds()));
    List<ExternalSearchCandidate> items =
        provider.search(request.query().strip(), count).stream()
            .map(
                result -> {
                  UUID candidateId = UUID.randomUUID();
                  String providerName =
                      result.siteName() == null || result.siteName().isBlank()
                          ? host(result.url())
                          : result.siteName();
                  ExternalSearchCandidate candidate =
                      new ExternalSearchCandidate(
                          candidateId,
                          result.title(),
                          result.url(),
                          result.snippet(),
                          providerName,
                          expiresAt);
                  candidates.put(
                      candidateId, new CachedCandidate(owner.getId(), result, expiresAt));
                  return candidate;
                })
            .toList();
    return new ExternalSearchResponse(items);
  }

  public KnowledgeItemResponse save(UUID candidateId, Authentication authentication) {
    var owner = currentOwnerService.require(authentication);
    CachedCandidate cached = candidates.getIfPresent(candidateId);
    if (cached == null
        || !cached.ownerId().equals(owner.getId())
        || !clock.instant().isBefore(cached.expiresAt())) {
      candidates.invalidate(candidateId);
      throw new ExternalSearchCandidateNotFoundException();
    }

    DeepSeekWebProvider.FetchedPage page = provider.fetch(cached.result().url());
    String title =
        firstNonBlank(page.title(), cached.result().title(), host(cached.result().url()));
    String content =
        page.content().length() > MAX_CONTENT_CHARS
            ? page.content().substring(0, MAX_CONTENT_CHARS)
            : page.content();
    var converted = textConverter.convert(content);
    String note =
        cached.result().summary() == null ? cached.result().snippet() : cached.result().summary();
    CreateKnowledgeRequest request =
        new CreateKnowledgeRequest(
            truncate(title, 300),
            converted.document(),
            truncate(note, 20_000),
            new BodySourceRequest("internet_result", cached.result().url(), clock.instant()));
    KnowledgeItemResponse saved = knowledgeService.create(request, authentication);
    candidates.invalidate(candidateId);
    return saved;
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.strip();
      }
    }
    return "Internet result";
  }

  private static String host(String url) {
    try {
      String host = java.net.URI.create(url).getHost();
      return host == null ? "Internet" : host;
    } catch (IllegalArgumentException error) {
      return "Internet";
    }
  }

  private static String truncate(String value, int max) {
    if (value == null) {
      return "";
    }
    return value.length() <= max ? value : value.substring(0, max);
  }

  private record CachedCandidate(
      UUID ownerId, DeepSeekWebProvider.SearchResult result, Instant expiresAt) {}
}

package site.seaunit.zhishiku.search.external;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.seaunit.zhishiku.billing.BillingService;
import site.seaunit.zhishiku.config.AppProperties;
import site.seaunit.zhishiku.consent.AiConsentService;
import site.seaunit.zhishiku.core.billing.BillingModel.Feature;
import site.seaunit.zhishiku.idempotency.IdempotencyService;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.BodySourceRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.CreateKnowledgeRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeService;
import site.seaunit.zhishiku.owner.CurrentOwnerService;
import site.seaunit.zhishiku.search.external.ExternalSearchDtos.ExternalSearchCandidate;
import site.seaunit.zhishiku.search.external.ExternalSearchDtos.ExternalSearchRequest;
import site.seaunit.zhishiku.search.external.ExternalSearchDtos.ExternalSearchResponse;
import site.seaunit.zhishiku.search.external.ExternalSearchDtos.ExternalSearchSaveResponse;
import site.seaunit.zhishiku.webimport.AiTextSecretRedactor;
import site.seaunit.zhishiku.webimport.ImportedKnowledgeMapper;
import site.seaunit.zhishiku.webimport.InternetResultOrganizer;
import site.seaunit.zhishiku.webimport.OrganizedKnowledge;
import site.seaunit.zhishiku.webimport.WebImportBillingRequiredException;
import site.seaunit.zhishiku.webimport.WebImportConsentRequiredException;
import site.seaunit.zhishiku.webimport.WebPageOrganizationException;

@Service
public class ExternalSearchService {
  private static final int MAX_CONTENT_CHARS = 200_000;

  private final DeepSeekWebProvider provider;
  private final KnowledgeService knowledgeService;
  private final InternetResultOrganizer organizer;
  private final DeepSeekAnswerService answerService;
  private final AiTextSecretRedactor secretRedactor;
  private final ImportedKnowledgeMapper knowledgeMapper;
  private final CurrentOwnerService currentOwnerService;
  private final AppProperties.ExternalSearch config;
  private final Clock clock;
  private final boolean allowUnbilledAi;
  private final BillingService billingService;
  private final AiConsentService aiConsents;
  private final IdempotencyService idempotency;
  private final Cache<UUID, CachedCandidate> candidates;

  public ExternalSearchService(
      DeepSeekWebProvider provider,
      KnowledgeService knowledgeService,
      InternetResultOrganizer organizer,
      DeepSeekAnswerService answerService,
      AiTextSecretRedactor secretRedactor,
      ImportedKnowledgeMapper knowledgeMapper,
      CurrentOwnerService currentOwnerService,
      AppProperties properties,
      @Value("${app.web-import.allow-unbilled-ai:false}") boolean allowUnbilledAi,
      BillingService billingService,
      AiConsentService aiConsents,
      IdempotencyService idempotency,
      Clock clock) {
    this.provider = provider;
    this.knowledgeService = knowledgeService;
    this.organizer = organizer;
    this.answerService = answerService;
    this.secretRedactor = secretRedactor;
    this.knowledgeMapper = knowledgeMapper;
    this.currentOwnerService = currentOwnerService;
    this.config = properties.externalSearch();
    this.allowUnbilledAi = allowUnbilledAi;
    this.billingService = billingService;
    this.aiConsents = aiConsents;
    this.idempotency = idempotency;
    this.clock = clock;
    this.candidates =
        Caffeine.newBuilder()
            .maximumSize(5_000)
            .expireAfterWrite(Duration.ofSeconds(Math.max(60, config.candidateTtlSeconds())))
            .build();
  }

  public ExternalSearchResponse search(
      ExternalSearchRequest request, Authentication authentication) {
    return search(request, null, authentication);
  }

  public ExternalSearchResponse search(
      ExternalSearchRequest request, String idempotencyKey, Authentication authentication) {
    String requestHash =
        IdempotencyService.hash(
            request.query()
                + "\n"
                + request.explicitConsent()
                + "\n"
                + request.includeAnswer()
                + "\n"
                + request.billingTaskId());
    if (idempotencyKey != null) {
      Optional<ExternalSearchResponse> replay =
          idempotency.replay(
              authentication,
              "internet_search",
              idempotencyKey,
              requestHash,
              ExternalSearchResponse.class);
      if (replay.isPresent()) {
        return replay.get();
      }
    }
    if (!request.explicitConsent() && !aiConsents.granted(authentication, "search")) {
      throw new IllegalArgumentException(
          "Explicit consent is required before sending a query to internet search.");
    }
    if (request.billingTaskId() != null) {
      billingService.requireReserved(authentication, request.billingTaskId(), "internet_search");
    } else if (!allowUnbilledAi) {
      throw new WebImportBillingRequiredException(
          "Paid AI billing must be enabled before internet search can run.");
    }
    try {
      var owner = currentOwnerService.require(authentication);
      int count = Math.max(1, Math.min(10, config.maxResults()));
      Instant expiresAt = clock.instant().plusSeconds(Math.max(60, config.candidateTtlSeconds()));
      List<DeepSeekWebProvider.SearchResult> results =
          provider.search(request.query().strip(), count);
      List<ExternalSearchCandidate> items =
          results.stream()
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
      String answer =
          request.includeAnswer() ? answerService.answer(request.query().strip(), results) : null;
      if (request.billingTaskId() != null) {
        Set<Feature> succeeded = new LinkedHashSet<>();
        succeeded.add(Feature.INTERNET_SEARCH);
        if (answer != null) {
          succeeded.add(Feature.AI_ANSWER);
        }
        billingService.settle(
            authentication, request.billingTaskId(), succeeded, "Internet search completed");
      }
      ExternalSearchResponse response = new ExternalSearchResponse(items, answer);
      if (idempotencyKey != null) {
        idempotency.record(
            authentication, "internet_search", idempotencyKey, requestHash, response, 200);
      }
      return response;
    } catch (RuntimeException error) {
      if (request.billingTaskId() != null) {
        billingService.releaseAfterFailure(
            authentication, request.billingTaskId(), "Internet search failed");
      }
      throw error;
    }
  }

  @Transactional
  public ExternalSearchSaveResponse save(
      UUID candidateId, boolean externalAiConsent, Authentication authentication) {
    return save(candidateId, externalAiConsent, null, authentication);
  }

  @Transactional
  public ExternalSearchSaveResponse save(
      UUID candidateId,
      boolean externalAiConsent,
      UUID billingTaskId,
      Authentication authentication) {
    return save(candidateId, externalAiConsent, billingTaskId, null, authentication);
  }

  @Transactional
  public ExternalSearchSaveResponse save(
      UUID candidateId,
      boolean externalAiConsent,
      UUID billingTaskId,
      String idempotencyKey,
      Authentication authentication) {
    String requestHash =
        IdempotencyService.hash(candidateId + "\n" + externalAiConsent + "\n" + billingTaskId);
    if (idempotencyKey != null) {
      Optional<ExternalSearchSaveResponse> replay =
          idempotency.replay(
              authentication,
              "internet_import",
              idempotencyKey,
              requestHash,
              ExternalSearchSaveResponse.class);
      if (replay.isPresent()) {
        return replay.get();
      }
    }
    if (!externalAiConsent && !aiConsents.granted(authentication, "search")) {
      throw new WebImportConsentRequiredException(
          "Explicit consent is required before sending the selected result to external AI.");
    }
    if (billingTaskId == null && !allowUnbilledAi) {
      throw new WebImportBillingRequiredException(
          "Paid AI billing must be enabled before internet result analysis can run.");
    }
    if (billingTaskId != null) {
      billingService.requireReserved(authentication, billingTaskId, "internet_import");
    }
    var owner = currentOwnerService.require(authentication);
    CachedCandidate cached = candidates.getIfPresent(candidateId);
    if (cached == null
        || !cached.ownerId().equals(owner.getId())
        || !clock.instant().isBefore(cached.expiresAt())) {
      candidates.invalidate(candidateId);
      throw new ExternalSearchCandidateNotFoundException();
    }

    try {
      DeepSeekWebProvider.FetchedPage page = provider.fetch(cached.result().url());
      String content =
          page.content().length() > MAX_CONTENT_CHARS
              ? page.content().substring(0, MAX_CONTENT_CHARS)
              : page.content();
      AiTextSecretRedactor.RedactionResult redacted = secretRedactor.redact(content);
      List<OrganizedKnowledge> organized =
          organizer.organize("internet search result", redacted.text());
      if (organized.isEmpty()) {
        throw new WebPageOrganizationException("AI returned no usable internet knowledge.");
      }
      List<KnowledgeItemResponse> created = new ArrayList<>();
      for (OrganizedKnowledge item : organized) {
        created.add(
            knowledgeMapper.withRedactionApplied(
                knowledgeService.createOrganized(
                    new CreateKnowledgeRequest(
                        item.title(),
                        knowledgeMapper.document(item.body()),
                        knowledgeMapper.note(
                            item,
                            redacted.applied(),
                            "",
                            "互联网搜索结果（" + firstNonBlank(cached.result().siteName(), "互联网") + "）"),
                        new BodySourceRequest(
                            "internet_result", cached.result().url(), clock.instant())),
                    item,
                    authentication),
                redacted.applied()));
      }
      if (billingTaskId != null) {
        billingService.settle(
            authentication, billingTaskId, Set.of(Feature.CLASSIFY), "Internet result organized");
      }
      candidates.invalidate(candidateId);
      ExternalSearchSaveResponse response = new ExternalSearchSaveResponse(List.copyOf(created));
      if (idempotencyKey != null) {
        idempotency.record(
            authentication, "internet_import", idempotencyKey, requestHash, response, 201);
      }
      return response;
    } catch (RuntimeException error) {
      if (billingTaskId != null) {
        billingService.releaseAfterFailure(
            authentication, billingTaskId, "Internet result organization failed");
      }
      throw error;
    }
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

  private record CachedCandidate(
      UUID ownerId, DeepSeekWebProvider.SearchResult result, Instant expiresAt) {}
}

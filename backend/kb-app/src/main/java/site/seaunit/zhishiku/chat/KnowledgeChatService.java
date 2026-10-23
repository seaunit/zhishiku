package site.seaunit.zhishiku.chat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import site.seaunit.zhishiku.billing.BillingService;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.AnswerRequest;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.AnswerResponse;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.ChatHistoryMessage;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.ChatSource;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.RetrievalResponse;
import site.seaunit.zhishiku.chat.KnowledgeChatModelClient.Evidence;
import site.seaunit.zhishiku.chat.KnowledgeChatModelClient.HistoryMessage;
import site.seaunit.zhishiku.chat.KnowledgeChatModelClient.ModelAnswer;
import site.seaunit.zhishiku.chat.KnowledgeChatRetrievalService.RetrievedKnowledge;
import site.seaunit.zhishiku.consent.AiConsentService;
import site.seaunit.zhishiku.core.billing.BillingModel.Feature;
import site.seaunit.zhishiku.idempotency.IdempotencyService;
import site.seaunit.zhishiku.search.external.DeepSeekWebProvider;
import site.seaunit.zhishiku.webimport.AiTextSecretRedactor;
import site.seaunit.zhishiku.webimport.WebImportBillingRequiredException;
import site.seaunit.zhishiku.webimport.WebImportConsentRequiredException;

@Service
public class KnowledgeChatService {
  private static final int MAX_HISTORY_CHARS = 8_000;
  private static final int MAX_INTERNET_EVIDENCE_CHARS = 2_000;
  private static final String INSUFFICIENT_EVIDENCE = "INSUFFICIENT_EVIDENCE";
  private static final Pattern CITATION = Pattern.compile("\\[(\\d{1,2})]");

  private final KnowledgeChatRetrievalService retrieval;
  private final KnowledgeChatModelClient model;
  private final DeepSeekWebProvider searchProvider;
  private final BillingService billing;
  private final AiConsentService consents;
  private final IdempotencyService idempotency;
  private final AiTextSecretRedactor redactor;
  private final boolean allowUnbilledAi;

  public KnowledgeChatService(
      KnowledgeChatRetrievalService retrieval,
      KnowledgeChatModelClient model,
      DeepSeekWebProvider searchProvider,
      BillingService billing,
      AiConsentService consents,
      IdempotencyService idempotency,
      AiTextSecretRedactor redactor,
      @Value("${app.web-import.allow-unbilled-ai:false}") boolean allowUnbilledAi) {
    this.retrieval = retrieval;
    this.model = model;
    this.searchProvider = searchProvider;
    this.billing = billing;
    this.consents = consents;
    this.idempotency = idempotency;
    this.redactor = redactor;
    this.allowUnbilledAi = allowUnbilledAi;
  }

  public RetrievalResponse retrieve(String query, Authentication authentication) {
    List<RetrievedKnowledge> found = retrieval.retrieve(query, authentication);
    return new RetrievalResponse(
        !found.isEmpty(), found.stream().map(RetrievedKnowledge::toSource).toList());
  }

  public AnswerResponse answer(
      AnswerRequest request, String idempotencyKey, Authentication authentication) {
    String requestHash =
        IdempotencyService.hash(
            request.query()
                + "\n"
                + historyFingerprint(request.history())
                + "\n"
                + request.externalAiConsent()
                + "\n"
                + request.billingTaskId());
    Optional<AnswerResponse> replay =
        idempotency.replay(
            authentication, "knowledge_chat", idempotencyKey, requestHash, AnswerResponse.class);
    if (replay.isPresent()) {
      return replay.get();
    }

    requireChatConsent(authentication, request.externalAiConsent());
    requireBilling(request.billingTaskId());
    if (request.billingTaskId() != null) {
      billing.requireReserved(authentication, request.billingTaskId(), "knowledge_chat");
    }

    try {
      List<HistoryMessage> history = sanitizeHistory(request.history());
      String question = sanitized(request.query(), 300);
      List<RetrievedKnowledge> found = smartRetrieve(request.query(), question, authentication);
      if (!found.isEmpty()) {
        List<Evidence> evidence = evidence(found);
        ModelAnswer answer = model.answer(question, history, evidence);
        if (usableAnswer(answer, evidence)) {
          Map<String, ChatSource> sourcesById = new LinkedHashMap<>();
          found.forEach(source -> sourcesById.put(source.knowledgeId().toString(), source.toSource()));
          List<ChatSource> usedSources =
              answer.usedSourceIds().stream().distinct().map(sourcesById::get).toList();
          settleIfPresent(
              authentication,
              request.billingTaskId(),
              Set.of(Feature.AI_ANSWER),
              "Knowledge answer");
          AnswerResponse response =
              new AnswerResponse(sanitized(answer.answer(), 6_000), false, usedSources, null);
          idempotency.record(
              authentication, "knowledge_chat", idempotencyKey, requestHash, response, 200);
          return response;
        }
      }

      ModelAnswer generalAnswer = model.answerGenerally(question, history);
      if (!usableGeneralAnswer(generalAnswer)) {
        releaseIfPresent(authentication, request.billingTaskId(), "No supported answer");
        AnswerResponse response = new AnswerResponse("", true, List.of(), "knowledge_insufficient");
        idempotency.record(
            authentication, "knowledge_chat", idempotencyKey, requestHash, response, 200);
        return response;
      }
      settleIfPresent(
          authentication, request.billingTaskId(), Set.of(Feature.AI_ANSWER), "General answer");
      AnswerResponse response =
          new AnswerResponse(sanitized(generalAnswer.answer(), 6_000), false, List.of(), null);
      idempotency.record(
          authentication, "knowledge_chat", idempotencyKey, requestHash, response, 200);
      return response;
    } catch (RuntimeException error) {
      releaseIfPresent(authentication, request.billingTaskId(), "Knowledge chat failed");
      throw error;
    }
  }

  public AnswerResponse streamAnswer(
      AnswerRequest request,
      String idempotencyKey,
      Authentication authentication,
      Consumer<String> tokenConsumer) {
    String requestHash =
        IdempotencyService.hash(
            request.query()
                + "\n"
                + historyFingerprint(request.history())
                + "\n"
                + request.externalAiConsent()
                + "\n"
                + request.billingTaskId());
    Optional<AnswerResponse> replay =
        idempotency.replay(
            authentication,
            "knowledge_chat_stream",
            idempotencyKey,
            requestHash,
            AnswerResponse.class);
    if (replay.isPresent()) {
      AnswerResponse response = replay.get();
      if (response.answer() != null && !response.answer().isBlank()) {
        tokenConsumer.accept(response.answer());
      }
      return response;
    }

    requireChatConsent(authentication, request.externalAiConsent());
    requireBilling(request.billingTaskId());
    if (request.billingTaskId() != null) {
      billing.requireReserved(authentication, request.billingTaskId(), "knowledge_chat");
    }

    try {
      List<HistoryMessage> history = sanitizeHistory(request.history());
      String question = sanitized(request.query(), 300);
      List<RetrievedKnowledge> found = smartRetrieve(request.query(), question, authentication);
      if (!found.isEmpty()) {
        List<Evidence> evidence = evidence(found);
        StringBuilder visibleKnowledge = new StringBuilder();
        String streamed =
            model.streamAnswer(
                question,
                history,
                evidence,
                token -> {
                  visibleKnowledge.append(token);
                  tokenConsumer.accept(token);
                });
        List<ChatSource> sources = citedSources(streamed, found);
        if (usableStreamAnswer(streamed, sources)) {
          settleIfPresent(
              authentication,
              request.billingTaskId(),
              Set.of(Feature.AI_ANSWER),
              "Knowledge answer");
          AnswerResponse response =
              new AnswerResponse(sanitized(streamed, 6_000), false, sources, null);
          idempotency.record(
              authentication, "knowledge_chat_stream", idempotencyKey, requestHash, response, 200);
          return response;
        }
        if (visibleKnowledge.length() > 0) {
          releaseIfPresent(
              authentication, request.billingTaskId(), "No supported streamed answer");
          AnswerResponse response = new AnswerResponse("", true, List.of(), "knowledge_insufficient");
          idempotency.record(
              authentication, "knowledge_chat_stream", idempotencyKey, requestHash, response, 200);
          return response;
        }
      }

      String generalAnswer = model.streamGeneralAnswer(question, history, tokenConsumer);
      if (!usableGeneralStreamAnswer(generalAnswer)) {
        releaseIfPresent(authentication, request.billingTaskId(), "No supported streamed answer");
        AnswerResponse response = new AnswerResponse("", true, List.of(), "knowledge_insufficient");
        idempotency.record(
            authentication, "knowledge_chat_stream", idempotencyKey, requestHash, response, 200);
        return response;
      }
      settleIfPresent(
          authentication, request.billingTaskId(), Set.of(Feature.AI_ANSWER), "General answer");
      AnswerResponse response =
          new AnswerResponse(sanitized(generalAnswer, 6_000), false, List.of(), null);
      idempotency.record(
          authentication, "knowledge_chat_stream", idempotencyKey, requestHash, response, 200);
      return response;
    } catch (RuntimeException error) {
      releaseIfPresent(authentication, request.billingTaskId(), "Knowledge chat stream failed");
      throw error;
    }
  }

  public AnswerResponse answerWithInternet(
      AnswerRequest request, String idempotencyKey, Authentication authentication) {
    String requestHash =
        IdempotencyService.hash(
            request.query()
                + "\n"
                + historyFingerprint(request.history())
                + "\n"
                + request.externalAiConsent()
                + "\n"
                + request.billingTaskId());
    Optional<AnswerResponse> replay =
        idempotency.replay(
            authentication,
            "knowledge_chat_internet",
            idempotencyKey,
            requestHash,
            AnswerResponse.class);
    if (replay.isPresent()) {
      return replay.get();
    }

    requireChatConsent(authentication, request.externalAiConsent());
    requireBilling(request.billingTaskId());
    if (request.billingTaskId() != null) {
      billing.requireReserved(
          authentication, request.billingTaskId(), "internet_search_with_answer");
    }

    boolean searchCompleted = false;
    try {
      String question = sanitized(request.query(), 300);
      List<DeepSeekWebProvider.SearchResult> results = searchProvider.search(question, 5);
      searchCompleted = true;
      if (results.isEmpty()) {
        settleIfPresent(
            authentication,
            request.billingTaskId(),
            Set.of(Feature.INTERNET_SEARCH),
            "Internet search returned no results");
        AnswerResponse response = new AnswerResponse("", false, List.of(), "internet_no_results");
        idempotency.record(
            authentication, "knowledge_chat_internet", idempotencyKey, requestHash, response, 200);
        return response;
      }

      List<Evidence> evidence =
          results.stream()
              .map(
                  result ->
                      new Evidence(
                          result.url(),
                          sanitized(result.title(), 300),
                          sanitized(
                              firstNonBlank(result.summary(), result.snippet(), result.content()),
                              MAX_INTERNET_EVIDENCE_CHARS),
                          result.url()))
              .filter(item -> !item.content().isBlank())
              .limit(5)
              .toList();
      if (evidence.isEmpty()) {
        settleIfPresent(
            authentication,
            request.billingTaskId(),
            Set.of(Feature.INTERNET_SEARCH),
            "Internet search returned no usable evidence");
        AnswerResponse response =
            new AnswerResponse("", false, internetSources(results), "internet_no_results");
        idempotency.record(
            authentication, "knowledge_chat_internet", idempotencyKey, requestHash, response, 200);
        return response;
      }

      ModelAnswer answer = model.answer(question, sanitizeHistory(request.history()), evidence);
      List<ChatSource> availableSources = internetSources(results);
      if (!usableAnswer(answer, evidence)) {
        settleIfPresent(
            authentication,
            request.billingTaskId(),
            Set.of(Feature.INTERNET_SEARCH),
            "Internet answer had insufficient evidence");
        AnswerResponse response =
            new AnswerResponse("", false, availableSources, "internet_insufficient_evidence");
        idempotency.record(
            authentication, "knowledge_chat_internet", idempotencyKey, requestHash, response, 200);
        return response;
      }

      Map<String, ChatSource> sourcesByUrl = new LinkedHashMap<>();
      availableSources.forEach(source -> sourcesByUrl.put(source.url(), source));
      List<ChatSource> usedSources =
          answer.usedSourceIds().stream().distinct().map(sourcesByUrl::get).toList();
      settleIfPresent(
          authentication,
          request.billingTaskId(),
          Set.of(Feature.INTERNET_SEARCH, Feature.AI_ANSWER),
          "Internet search and answer completed");
      AnswerResponse response =
          new AnswerResponse(sanitized(answer.answer(), 6_000), false, usedSources, null);
      idempotency.record(
          authentication, "knowledge_chat_internet", idempotencyKey, requestHash, response, 200);
      return response;
    } catch (RuntimeException error) {
      if (request.billingTaskId() != null && searchCompleted) {
        billing.settle(
            authentication,
            request.billingTaskId(),
            Set.of(Feature.INTERNET_SEARCH),
            "Internet search completed but answer failed");
      } else {
        releaseIfPresent(authentication, request.billingTaskId(), "Internet chat failed");
      }
      throw error;
    }
  }

  public AnswerResponse streamInternetAnswer(
      AnswerRequest request,
      String idempotencyKey,
      Authentication authentication,
      Consumer<String> tokenConsumer) {
    String requestHash =
        IdempotencyService.hash(
            request.query()
                + "\n"
                + historyFingerprint(request.history())
                + "\n"
                + request.externalAiConsent()
                + "\n"
                + request.billingTaskId());
    Optional<AnswerResponse> replay =
        idempotency.replay(
            authentication,
            "knowledge_chat_internet_stream",
            idempotencyKey,
            requestHash,
            AnswerResponse.class);
    if (replay.isPresent()) {
      AnswerResponse response = replay.get();
      if (response.answer() != null && !response.answer().isBlank()) {
        tokenConsumer.accept(response.answer());
      }
      return response;
    }

    requireChatConsent(authentication, request.externalAiConsent());
    requireBilling(request.billingTaskId());
    if (request.billingTaskId() != null) {
      billing.requireReserved(
          authentication, request.billingTaskId(), "internet_search_with_answer");
    }

    boolean searchCompleted = false;
    try {
      String question = sanitized(request.query(), 300);
      List<DeepSeekWebProvider.SearchResult> results = searchProvider.search(question, 5);
      searchCompleted = true;
      List<DeepSeekWebProvider.SearchResult> usableResults =
          results.stream()
              .filter(
                  result ->
                      !firstNonBlank(result.summary(), result.snippet(), result.content())
                          .isBlank())
              .limit(5)
              .toList();
      if (usableResults.isEmpty()) {
        settleIfPresent(
            authentication,
            request.billingTaskId(),
            Set.of(Feature.INTERNET_SEARCH),
            "Internet search returned no results");
        AnswerResponse response =
            new AnswerResponse("", false, internetSources(results), "internet_no_results");
        idempotency.record(
            authentication,
            "knowledge_chat_internet_stream",
            idempotencyKey,
            requestHash,
            response,
            200);
        return response;
      }
      List<Evidence> evidence =
          usableResults.stream()
              .map(
                  result ->
                      new Evidence(
                          result.url(),
                          sanitized(result.title(), 300),
                          sanitized(
                              firstNonBlank(result.summary(), result.snippet(), result.content()),
                              MAX_INTERNET_EVIDENCE_CHARS),
                          result.url()))
              .toList();
      String streamed =
          model.streamAnswer(question, sanitizeHistory(request.history()), evidence, tokenConsumer);
      List<ChatSource> availableSources = internetSources(usableResults);
      List<ChatSource> usedSources = citedInternetSources(streamed, availableSources);
      if (!usableStreamAnswer(streamed, usedSources)) {
        settleIfPresent(
            authentication,
            request.billingTaskId(),
            Set.of(Feature.INTERNET_SEARCH),
            "Internet answer had insufficient evidence");
        AnswerResponse response =
            new AnswerResponse("", false, availableSources, "internet_insufficient_evidence");
        idempotency.record(
            authentication,
            "knowledge_chat_internet_stream",
            idempotencyKey,
            requestHash,
            response,
            200);
        return response;
      }
      settleIfPresent(
          authentication,
          request.billingTaskId(),
          Set.of(Feature.INTERNET_SEARCH, Feature.AI_ANSWER),
          "Internet search and answer completed");
      AnswerResponse response =
          new AnswerResponse(sanitized(streamed, 6_000), false, usedSources, null);
      idempotency.record(
          authentication,
          "knowledge_chat_internet_stream",
          idempotencyKey,
          requestHash,
          response,
          200);
      return response;
    } catch (RuntimeException error) {
      if (request.billingTaskId() != null && searchCompleted) {
        billing.settle(
            authentication,
            request.billingTaskId(),
            Set.of(Feature.INTERNET_SEARCH),
            "Internet search completed but answer failed");
      } else {
        releaseIfPresent(authentication, request.billingTaskId(), "Internet chat stream failed");
      }
      throw error;
    }
  }

  private List<RetrievedKnowledge> smartRetrieve(
      String query, String sanitizedQuestion, Authentication authentication) {
    List<RetrievedKnowledge> found = retrieval.retrieve(query, authentication);
    if (found.isEmpty() && retrieval.hasActiveKnowledge(authentication)) {
      List<String> plannedQueries = model.planQueries(sanitizedQuestion);
      if (!plannedQueries.isEmpty()) {
        found = retrieval.retrieve(query, plannedQueries, authentication);
      }
    }
    return found;
  }

  private List<Evidence> evidence(List<RetrievedKnowledge> found) {
    return found.stream()
        .map(
            source ->
                new Evidence(
                    source.knowledgeId().toString(),
                    sanitized(source.title(), 300),
                    sanitized(source.content(), 3_500),
                    null))
        .toList();
  }

  private List<ChatSource> citedSources(String answer, List<RetrievedKnowledge> found) {
    LinkedHashSet<Integer> indexes = new LinkedHashSet<>();
    Matcher matcher = CITATION.matcher(answer);
    while (matcher.find()) {
      int index = Integer.parseInt(matcher.group(1));
      if (index >= 1 && index <= found.size()) {
        indexes.add(index);
      }
    }
    return indexes.stream().map(index -> found.get(index - 1).toSource()).toList();
  }

  private static List<ChatSource> citedInternetSources(
      String answer, List<ChatSource> availableSources) {
    LinkedHashSet<Integer> indexes = new LinkedHashSet<>();
    Matcher matcher = CITATION.matcher(answer);
    while (matcher.find()) {
      int index = Integer.parseInt(matcher.group(1));
      if (index >= 1 && index <= availableSources.size()) {
        indexes.add(index);
      }
    }
    return indexes.stream().map(index -> availableSources.get(index - 1)).toList();
  }

  private static boolean usableStreamAnswer(String answer, List<ChatSource> sources) {
    return answer != null
        && !answer.isBlank()
        && !INSUFFICIENT_EVIDENCE.equals(answer.strip())
        && !sources.isEmpty();
  }

  private void requireChatConsent(Authentication authentication, boolean explicitConsent) {
    if (!explicitConsent && !consents.granted(authentication, "chat")) {
      throw new WebImportConsentRequiredException(
          "Explicit consent is required before sending chat content to external AI.");
    }
  }

  private void requireBilling(java.util.UUID billingTaskId) {
    if (billingTaskId == null && !allowUnbilledAi) {
      throw new WebImportBillingRequiredException(
          "Paid AI billing must be enabled before knowledge chat can run.");
    }
  }

  private void settleIfPresent(
      Authentication authentication,
      java.util.UUID billingTaskId,
      Set<Feature> features,
      String reason) {
    if (billingTaskId != null) {
      billing.settle(authentication, billingTaskId, features, reason);
    }
  }

  private void releaseIfPresent(
      Authentication authentication, java.util.UUID billingTaskId, String reason) {
    if (billingTaskId != null) {
      billing.releaseAfterFailure(authentication, billingTaskId, reason);
    }
  }

  private static boolean usableAnswer(ModelAnswer answer, List<Evidence> evidence) {
    if (answer == null
        || answer.needsInternet()
        || answer.answer() == null
        || answer.answer().isBlank()
        || answer.usedSourceIds().isEmpty()) {
      return false;
    }
    Set<String> allowed = new LinkedHashSet<>();
    evidence.forEach(item -> allowed.add(item.id()));
    return answer.usedSourceIds().stream().allMatch(allowed::contains);
  }

  private static boolean usableGeneralAnswer(ModelAnswer answer) {
    return answer != null
        && !answer.needsInternet()
        && answer.answer() != null
        && !answer.answer().isBlank()
        && !INSUFFICIENT_EVIDENCE.equals(answer.answer().strip());
  }

  private static boolean usableGeneralStreamAnswer(String answer) {
    return answer != null
        && !answer.isBlank()
        && !INSUFFICIENT_EVIDENCE.equals(answer.strip())
        && !"NEEDS_INTERNET".equals(answer.strip());
  }

  private List<HistoryMessage> sanitizeHistory(List<ChatHistoryMessage> history) {
    List<HistoryMessage> result = new ArrayList<>();
    int remaining = MAX_HISTORY_CHARS;
    for (int index = Math.max(0, history.size() - 6);
        index < history.size() && remaining > 0;
        index++) {
      ChatHistoryMessage message = history.get(index);
      String content = redactor.redact(message.content()).text().strip();
      if (content.isBlank()) {
        continue;
      }
      content = content.substring(0, Math.min(content.length(), Math.min(2_000, remaining)));
      result.add(new HistoryMessage(message.role(), content));
      remaining -= content.length();
    }
    return List.copyOf(result);
  }

  private String sanitized(String value, int maxLength) {
    String redacted = redactor.redact(value == null ? "" : value).text().strip();
    return redacted.substring(0, Math.min(redacted.length(), maxLength));
  }

  private static String historyFingerprint(List<ChatHistoryMessage> history) {
    return history.stream()
        .map(message -> message.role() + ":" + IdempotencyService.hash(message.content()))
        .reduce("", (left, right) -> left + "\n" + right);
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return "";
  }

  private static List<ChatSource> internetSources(List<DeepSeekWebProvider.SearchResult> results) {
    return results.stream()
        .limit(5)
        .map(
            result ->
                new ChatSource(
                    "internet",
                    null,
                    result.title(),
                    firstNonBlank(result.summary(), result.snippet()),
                    result.url(),
                    firstNonBlank(result.siteName(), host(result.url())),
                    List.of()))
        .toList();
  }

  private static String host(String url) {
    try {
      String host = java.net.URI.create(url).getHost();
      return host == null ? "Internet" : host;
    } catch (IllegalArgumentException error) {
      return "Internet";
    }
  }
}

package site.seaunit.zhishiku.chat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.SearchHit;
import site.seaunit.zhishiku.knowledge.KnowledgeService;

@Service
public class KnowledgeChatRetrievalService {
  private static final int MAX_SOURCES = 8;
  private static final int MAX_EVIDENCE_CHARS = 3_500;
  private static final Pattern TRAILING_QUESTION =
      Pattern.compile("(?i)\\s*(?:是什么|有哪些|为什么|怎么样|怎么|如何|多少|吗|呢)\\s*[?？。.!！]*$");
  private static final Pattern LEADING_QUESTION =
      Pattern.compile("(?i)^\\s*(?:请问|请帮我|帮我|我想知道|想问一下|想问|什么是|何谓|何为|如何|怎么|为什么|有哪些)\\s*");
  private static final Pattern EDGE_PUNCTUATION =
      Pattern.compile("^[\\s，,。？?！!；;：:]+|[\\s，,。？?！!；;：:]+$");
  private static final Pattern TOKEN_SEPARATOR = Pattern.compile("[\\s，,。？?！!；;：:]+");
  private static final Set<String> STOP_WORDS =
      Set.of(
          "what", "which", "where", "when", "who", "why", "how", "the", "and", "please", "请问", "帮我",
          "一下");

  private final KnowledgeService knowledgeService;

  public KnowledgeChatRetrievalService(KnowledgeService knowledgeService) {
    this.knowledgeService = knowledgeService;
  }

  @Transactional(readOnly = true)
  public List<RetrievedKnowledge> retrieve(String query, Authentication authentication) {
    return retrieve(query, List.of(), authentication);
  }

  @Transactional(readOnly = true)
  public List<RetrievedKnowledge> retrieve(
      String query, List<String> plannedQueries, Authentication authentication) {
    Map<UUID, SearchHit> hits = new LinkedHashMap<>();
    LinkedHashSet<String> candidateQueries = new LinkedHashSet<>(retrievalQueries(query));
    for (String planned : plannedQueries) {
      candidateQueries.addAll(retrievalQueries(planned));
    }
    for (String candidateQuery : candidateQueries) {
      for (SearchHit hit :
          knowledgeService.search(candidateQuery, 1, MAX_SOURCES, "all", authentication).items()) {
        hits.putIfAbsent(hit.item().id(), hit);
        if (hits.size() >= MAX_SOURCES) {
          break;
        }
      }
      if (hits.size() >= MAX_SOURCES) {
        break;
      }
    }
    List<RetrievedKnowledge> result = new ArrayList<>(hits.size());
    for (SearchHit hit : hits.values()) {
      UUID id = hit.item().id();
      KnowledgeItemResponse detail;
      try {
        detail = knowledgeService.detail(id, authentication);
      } catch (IllegalArgumentException ignored) {
        continue;
      }
      String content = limit(detail.body(), MAX_EVIDENCE_CHARS);
      if (content.isBlank()) {
        content = limit(detail.summary(), MAX_EVIDENCE_CHARS);
      }
      if (content.isBlank()) {
        continue;
      }
      result.add(
          new RetrievedKnowledge(
              id,
              detail.title(),
              excerpt(detail.summary(), detail.body()),
              content,
              detail.category() == null ? null : detail.category().name(),
              detail.tags().stream().map(tag -> tag.name()).toList(),
              detail.source() == null ? null : detail.source().kind(),
              detail.source() == null ? null : detail.source().url(),
              hit.matchedFields()));
    }
    return List.copyOf(result);
  }

  @Transactional(readOnly = true)
  public boolean hasActiveKnowledge(Authentication authentication) {
    return knowledgeService.list(1, 1, "all", "all", authentication).pageInfo().totalItems() > 0;
  }

  static List<String> retrievalQueries(String query) {
    String normalized = query == null ? "" : query.strip();
    if (normalized.isBlank()) {
      return List.of();
    }
    List<String> queries = new ArrayList<>();
    String withoutPunctuation = EDGE_PUNCTUATION.matcher(normalized).replaceAll("").strip();
    addQuery(queries, withoutPunctuation);
    String withoutLeading = LEADING_QUESTION.matcher(withoutPunctuation).replaceFirst("").strip();
    addQuery(queries, withoutLeading);
    String withoutQuestion = TRAILING_QUESTION.matcher(withoutLeading).replaceFirst("").strip();
    addQuery(queries, withoutQuestion);
    for (String token : TOKEN_SEPARATOR.split(withoutQuestion)) {
      String value = token.strip();
      if (value.length() >= 2 && !STOP_WORDS.contains(value.toLowerCase(Locale.ROOT))) {
        addQuery(queries, value);
      }
    }
    return List.copyOf(queries);
  }

  private static void addQuery(List<String> queries, String value) {
    if (!value.isBlank() && !queries.contains(value)) {
      queries.add(value);
    }
  }

  private static String excerpt(String summary, String body) {
    String value = summary == null || summary.isBlank() ? body : summary;
    String normalized = value == null ? "" : value.replaceAll("\\s+", " ").strip();
    if (normalized.length() <= 260) {
      return normalized;
    }
    return normalized.substring(0, 257) + "...";
  }

  private static String limit(String value, int maxLength) {
    String normalized = value == null ? "" : value.strip();
    return normalized.substring(0, Math.min(normalized.length(), maxLength));
  }

  public record RetrievedKnowledge(
      UUID knowledgeId,
      String title,
      String excerpt,
      String content,
      String category,
      List<String> tags,
      String sourceKind,
      String sourceUrl,
      List<String> matchedFields) {
    public RetrievedKnowledge {
      tags = tags == null ? List.of() : List.copyOf(tags);
      matchedFields = matchedFields == null ? List.of() : List.copyOf(matchedFields);
    }

    public KnowledgeChatDtos.ChatSource toSource() {
      return new KnowledgeChatDtos.ChatSource(
          "knowledge", knowledgeId, title, excerpt, sourceUrl, null, matchedFields);
    }
  }
}

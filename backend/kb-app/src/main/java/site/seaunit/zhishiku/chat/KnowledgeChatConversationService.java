package site.seaunit.zhishiku.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.AppendMessageRequest;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.ChatConversationDetail;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.ChatConversationPage;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.ChatConversationResponse;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.ChatMessageResponse;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.ChatSource;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.ConversationDeleteResponse;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.CreateConversationRequest;
import site.seaunit.zhishiku.idempotency.IdempotencyService;
import site.seaunit.zhishiku.owner.CurrentOwnerService;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;

@Service
public class KnowledgeChatConversationService {
  private static final int MAX_CONVERSATIONS = 20;
  private static final int MAX_MESSAGES = 200;
  private static final TypeReference<List<ChatSource>> SOURCE_LIST = new TypeReference<>() {};

  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;
  private final CurrentOwnerService owners;
  private final IdempotencyService idempotency;

  public KnowledgeChatConversationService(
      JdbcTemplate jdbc,
      ObjectMapper mapper,
      CurrentOwnerService owners,
      IdempotencyService idempotency) {
    this.jdbc = jdbc;
    this.mapper = mapper;
    this.owners = owners;
    this.idempotency = idempotency;
  }

  @Transactional(readOnly = true)
  public ChatConversationPage list(Authentication authentication, int limit) {
    OwnerAccountEntity owner = owners.require(authentication);
    int pageSize = Math.min(Math.max(limit, 1), MAX_CONVERSATIONS);
    List<ChatConversationResponse> items =
        jdbc.query(
            """
            SELECT c.id, c.title, c.created_at, c.updated_at,
                   (SELECT count(*) FROM chat_message m WHERE m.conversation_id = c.id) AS message_count,
                   (SELECT m.content FROM chat_message m
                    WHERE m.conversation_id = c.id
                    ORDER BY m.created_at DESC, m.id DESC LIMIT 1) AS last_message
            FROM chat_conversation c
            WHERE c.owner_id = ?
            ORDER BY c.updated_at DESC, c.id DESC
            LIMIT ?
            """,
            (result, row) ->
                new ChatConversationResponse(
                    result.getObject("id", UUID.class),
                    result.getString("title"),
                    preview(result.getString("last_message")),
                    result.getLong("message_count"),
                    result.getTimestamp("created_at").toInstant(),
                    result.getTimestamp("updated_at").toInstant()),
            owner.getId(),
            pageSize);
    return new ChatConversationPage(items);
  }

  @Transactional
  public ChatConversationResponse create(
      Authentication authentication, CreateConversationRequest request, String idempotencyKey) {
    OwnerAccountEntity owner = owners.require(authentication);
    String title = normalizeTitle(request.title());
    String requestHash = IdempotencyService.hash(title);
    Optional<ChatConversationResponse> replay =
        idempotency.replay(
            authentication,
            "chat_conversation_create",
            idempotencyKey,
            requestHash,
            ChatConversationResponse.class);
    if (replay.isPresent()) {
      return replay.get();
    }

    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    jdbc.update(
        """
        INSERT INTO chat_conversation (id, owner_id, title, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?)
        """,
        id,
        owner.getId(),
        title,
        Timestamp.from(now),
        Timestamp.from(now));
    ChatConversationResponse response = new ChatConversationResponse(id, title, "", 0, now, now);
    idempotency.record(
        authentication, "chat_conversation_create", idempotencyKey, requestHash, response, 201);
    return response;
  }

  @Transactional(readOnly = true)
  public ChatConversationDetail detail(Authentication authentication, UUID conversationId) {
    OwnerAccountEntity owner = owners.require(authentication);
    ChatConversationResponse conversation = conversation(owner.getId(), conversationId);
    List<ChatMessageResponse> messages =
        new ArrayList<>(
            jdbc.query(
                """
                SELECT *
                FROM (
                  SELECT id, role, content, sources, needs_internet, reason, created_at
                  FROM chat_message
                  WHERE conversation_id = ? AND owner_id = ?
                  ORDER BY created_at DESC, id DESC
                  LIMIT ?
                ) recent
                ORDER BY created_at, id
                """,
                (result, row) ->
                    new ChatMessageResponse(
                        result.getObject("id", UUID.class),
                        result.getString("role"),
                        result.getString("content"),
                        parseSources(result.getString("sources")),
                        result.getBoolean("needs_internet"),
                        result.getString("reason"),
                        result.getTimestamp("created_at").toInstant()),
                conversationId,
                owner.getId(),
                MAX_MESSAGES));
    return new ChatConversationDetail(conversation, messages);
  }

  @Transactional
  public ChatMessageResponse append(
      Authentication authentication,
      UUID conversationId,
      AppendMessageRequest request,
      String idempotencyKey) {
    OwnerAccountEntity owner = owners.require(authentication);
    List<ChatSource> sources = sanitizeSources(request.sources());
    String sourcesJson = writeSources(sources);
    String requestHash =
        IdempotencyService.hash(
            conversationId
                + "\n"
                + request.role()
                + "\n"
                + request.content()
                + "\n"
                + sourcesJson
                + "\n"
                + request.needsInternet()
                + "\n"
                + (request.reason() == null ? "" : request.reason()));
    Optional<ChatMessageResponse> replay =
        idempotency.replay(
            authentication,
            "chat_message_append",
            idempotencyKey,
            requestHash,
            ChatMessageResponse.class);
    if (replay.isPresent()) {
      return replay.get();
    }

    lockConversation(owner.getId(), conversationId);
    UUID messageId = UUID.randomUUID();
    Instant now = Instant.now();
    jdbc.update(
        """
        INSERT INTO chat_message
          (id, conversation_id, owner_id, role, content, sources,
           needs_internet, reason, created_at)
        VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?)
        """,
        messageId,
        conversationId,
        owner.getId(),
        request.role(),
        request.content().strip(),
        sourcesJson,
        request.needsInternet(),
        request.reason(),
        Timestamp.from(now));
    jdbc.update(
        """
        UPDATE chat_conversation
        SET updated_at = ?
        WHERE id = ? AND owner_id = ?
        """,
        Timestamp.from(now),
        conversationId,
        owner.getId());
    ChatMessageResponse response =
        new ChatMessageResponse(
            messageId,
            request.role(),
            request.content().strip(),
            sources,
            request.needsInternet(),
            request.reason(),
            now);
    idempotency.record(
        authentication, "chat_message_append", idempotencyKey, requestHash, response, 201);
    return response;
  }

  @Transactional
  public ConversationDeleteResponse delete(
      Authentication authentication, UUID conversationId, String idempotencyKey) {
    OwnerAccountEntity owner = owners.require(authentication);
    String requestHash = IdempotencyService.hash(conversationId.toString());
    Optional<ConversationDeleteResponse> replay =
        idempotency.replay(
            authentication,
            "chat_conversation_delete",
            idempotencyKey,
            requestHash,
            ConversationDeleteResponse.class);
    if (replay.isPresent()) {
      return replay.get();
    }
    int deleted =
        jdbc.update(
            "DELETE FROM chat_conversation WHERE id = ? AND owner_id = ?",
            conversationId,
            owner.getId());
    ConversationDeleteResponse response = new ConversationDeleteResponse(deleted == 1);
    idempotency.record(
        authentication, "chat_conversation_delete", idempotencyKey, requestHash, response, 200);
    return response;
  }

  private ChatConversationResponse conversation(UUID ownerId, UUID conversationId) {
    return jdbc
        .query(
            """
            SELECT c.id, c.title, c.created_at, c.updated_at,
                   (SELECT count(*) FROM chat_message m WHERE m.conversation_id = c.id) AS message_count,
                   (SELECT m.content FROM chat_message m
                    WHERE m.conversation_id = c.id
                    ORDER BY m.created_at DESC, m.id DESC LIMIT 1) AS last_message
            FROM chat_conversation c
            WHERE c.id = ? AND c.owner_id = ?
            """,
            (result, row) ->
                new ChatConversationResponse(
                    result.getObject("id", UUID.class),
                    result.getString("title"),
                    preview(result.getString("last_message")),
                    result.getLong("message_count"),
                    result.getTimestamp("created_at").toInstant(),
                    result.getTimestamp("updated_at").toInstant()),
            conversationId,
            ownerId)
        .stream()
        .findFirst()
        .orElseThrow(KnowledgeChatConversationNotFoundException::new);
  }

  private void lockConversation(UUID ownerId, UUID conversationId) {
    List<UUID> locked =
        jdbc.query(
            "SELECT id FROM chat_conversation WHERE id = ? AND owner_id = ? FOR UPDATE",
            (result, row) -> result.getObject("id", UUID.class),
            conversationId,
            ownerId);
    if (locked.isEmpty()) {
      throw new KnowledgeChatConversationNotFoundException();
    }
  }

  private List<ChatSource> sanitizeSources(List<ChatSource> sources) {
    if (sources == null || sources.isEmpty()) {
      return List.of();
    }
    return sources.stream()
        .limit(8)
        .map(
            source ->
                new ChatSource(
                    "internet".equals(source.kind()) ? "internet" : "knowledge",
                    source.knowledgeId(),
                    safe(source.title(), 300),
                    safe(source.excerpt(), 1_000),
                    safeHttpUrl(source.url()),
                    safe(source.provider(), 200),
                    source.matchedFields().stream()
                        .limit(8)
                        .map(value -> safe(value, 80))
                        .toList()))
        .toList();
  }

  private String writeSources(List<ChatSource> sources) {
    try {
      return mapper.writeValueAsString(sources);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Chat sources could not be serialized.", error);
    }
  }

  private List<ChatSource> parseSources(String value) {
    try {
      return mapper.readValue(value, SOURCE_LIST);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Stored chat sources are invalid.", error);
    }
  }

  private static String normalizeTitle(String value) {
    String normalized = value.replaceAll("[\\p{Cc}]+", " ").replaceAll("\\s+", " ").strip();
    return normalized.substring(0, Math.min(normalized.length(), 100));
  }

  private static String safe(String value, int maxLength) {
    if (value == null) {
      return null;
    }
    String normalized = value.replaceAll("[\\p{Cc}]+", " ").strip();
    return normalized.substring(0, Math.min(normalized.length(), maxLength));
  }

  static String safeHttpUrl(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      java.net.URI uri = java.net.URI.create(value.strip());
      if (uri.getHost() == null || uri.getUserInfo() != null) {
        return null;
      }
      String scheme = uri.getScheme();
      return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)
          ? uri.toString()
          : null;
    } catch (IllegalArgumentException error) {
      return null;
    }
  }

  private static String preview(String value) {
    if (value == null) {
      return "";
    }
    String normalized = value.replaceAll("\\s+", " ").strip();
    return normalized.length() <= 160 ? normalized : normalized.substring(0, 157) + "...";
  }
}

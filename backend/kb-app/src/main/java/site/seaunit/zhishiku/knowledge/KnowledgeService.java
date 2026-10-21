package site.seaunit.zhishiku.knowledge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.seaunit.zhishiku.attachment.AttachmentEntity;
import site.seaunit.zhishiku.attachment.AttachmentRepository;
import site.seaunit.zhishiku.core.richtext.PlainTextProjector;
import site.seaunit.zhishiku.core.richtext.RichTextValidator;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.BodySourceRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.CreateKnowledgeRequest;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeSummaryResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.SearchHit;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.SearchResponse;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.SourceSummary;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.UpdateKnowledgeRequest;
import site.seaunit.zhishiku.owner.CurrentOwnerService;
import site.seaunit.zhishiku.owner.OwnerAccountEntity;
import site.seaunit.zhishiku.richtext.RichDocumentEntity;
import site.seaunit.zhishiku.richtext.RichDocumentRepository;
import site.seaunit.zhishiku.richtext.RichImageUploadEntity;
import site.seaunit.zhishiku.richtext.RichImageUploadRepository;

@Service
public class KnowledgeService {
  private static final long MAX_TOTAL_IMAGE_BYTES = 50L * 1024 * 1024;
  private static final String ACTIVE = "active";
  private static final String CONVERTER_VERSION = "richtext-java-v1";

  private final CurrentOwnerService currentOwnerService;
  private final KnowledgeItemRepository items;
  private final RichDocumentRepository documents;
  private final RichImageUploadRepository uploads;
  private final AttachmentRepository attachments;
  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;
  private final RichTextValidator validator;
  private final PlainTextProjector projector;

  public KnowledgeService(
      CurrentOwnerService currentOwnerService,
      KnowledgeItemRepository items,
      RichDocumentRepository documents,
      RichImageUploadRepository uploads,
      AttachmentRepository attachments,
      JdbcTemplate jdbc,
      ObjectMapper mapper) {
    this.currentOwnerService = currentOwnerService;
    this.items = items;
    this.documents = documents;
    this.uploads = uploads;
    this.attachments = attachments;
    this.jdbc = jdbc;
    this.mapper = mapper;
    this.validator = RichTextValidator.defaults();
    this.projector = new PlainTextProjector();
  }

  @Transactional
  public KnowledgeItemResponse create(
      CreateKnowledgeRequest request, Authentication authentication) {
    OwnerAccountEntity owner = currentOwnerService.require(authentication);
    PreparedDocument prepared = prepare(request.bodyDocument(), owner, null);
    KnowledgeItemEntity item =
        items.saveAndFlush(
            KnowledgeItemEntity.create(
                owner.getId(), request.title(), prepared.plainText(), request.note()));
    RichDocumentEntity document =
        RichDocumentEntity.create(
            item.getId(),
            prepared.document(),
            prepared.plainText(),
            prepared.hash(),
            prepared.images().size(),
            prepared.totalImageBytes(),
            item.getVersion());
    documents.save(document);
    UUID revisionId = insertRevision(item, prepared, "create", 1);
    insertImageReferences(item, revisionId, prepared.images());
    insertSource(item, request.bodySource(), prepared.hash());
    updateSearch(item, prepared.plainText());
    return response(item, document);
  }

  @Transactional
  public KnowledgeItemResponse update(
      UUID itemId, UpdateKnowledgeRequest request, Authentication authentication) {
    OwnerAccountEntity owner = currentOwnerService.require(authentication);
    KnowledgeItemEntity item =
        items
            .findByIdAndOwnerIdAndLifecycleState(itemId, owner.getId(), ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge item was not found."));
    if (item.getVersion() != request.expectedVersion()) {
      throw new RichTextConflictException(item.getVersion());
    }

    PreparedDocument prepared = prepare(request.bodyDocument(), owner, item.getId());
    item.update(request.title(), prepared.plainText(), request.note());
    item = items.saveAndFlush(item);
    RichDocumentEntity document =
        documents
            .findById(item.getId())
            .orElseThrow(() -> new IllegalStateException("Rich document is missing."));
    document.update(
        prepared.document(),
        prepared.plainText(),
        prepared.hash(),
        prepared.images().size(),
        prepared.totalImageBytes(),
        item.getVersion());
    documents.saveAndFlush(document);
    int nextRevision =
        jdbc.queryForObject(
                "SELECT COALESCE(MAX(revision_no), 0) + 1 FROM rich_document_revision WHERE item_id = ?",
                Integer.class,
                item.getId())
            .intValue();
    UUID revisionId = insertRevision(item, prepared, "edit", nextRevision);
    insertImageReferences(item, revisionId, prepared.images());
    updateSearch(item, prepared.plainText());
    return response(item, document);
  }

  @Transactional(readOnly = true)
  public KnowledgeItemResponse detail(UUID itemId, Authentication authentication) {
    OwnerAccountEntity owner = currentOwnerService.require(authentication);
    KnowledgeItemEntity item =
        items
            .findByIdAndOwnerIdAndLifecycleState(itemId, owner.getId(), ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge item was not found."));
    RichDocumentEntity document =
        documents
            .findById(item.getId())
            .orElseThrow(() -> new IllegalStateException("Rich document is missing."));
    return response(item, document);
  }

  @Transactional(readOnly = true)
  public List<KnowledgeSummaryResponse> list(int limit, Authentication authentication) {
    OwnerAccountEntity owner = currentOwnerService.require(authentication);
    return items
        .findByOwnerIdAndLifecycleStateOrderByUpdatedAtDesc(
            owner.getId(), ACTIVE, PageRequest.of(0, Math.min(Math.max(limit, 1), 100)))
        .stream()
        .map(KnowledgeService::summary)
        .toList();
  }

  @Transactional(readOnly = true)
  public SearchResponse search(String query, int limit, Authentication authentication) {
    OwnerAccountEntity owner = currentOwnerService.require(authentication);
    String safeQuery = query == null ? "" : query.strip();
    if (safeQuery.isBlank()) {
      return new SearchResponse("", List.of(), null, false);
    }
    String pattern = "%" + safeQuery.toLowerCase(Locale.ROOT) + "%";
    List<KnowledgeSummaryResponse> results =
        jdbc.query(
            """
            SELECT k.id, k.title, k.body, k.version, k.created_at, k.updated_at
            FROM knowledge_item k
            JOIN search_document s ON s.item_id = k.id
            WHERE k.owner_id = ?
              AND k.lifecycle_state = 'active'
              AND (lower(s.search_text) LIKE ? OR similarity(s.search_text, ?) > 0.12)
            ORDER BY
              CASE WHEN lower(k.title) = ? THEN 0
                   WHEN lower(k.title) LIKE ? THEN 1
                   ELSE 2 END,
              similarity(s.search_text, ?) DESC,
              k.updated_at DESC
            LIMIT ?
            """,
            (rs, rowNum) ->
                new KnowledgeSummaryResponse(
                    rs.getObject("id", UUID.class),
                    rs.getString("title"),
                    preview(rs.getString("body")),
                    null,
                    "not_requested",
                    ACTIVE,
                    new SourceSummary("manual", "manual", null, null),
                    null,
                    List.of(),
                    rs.getLong("version"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()),
            owner.getId(),
            pattern,
            safeQuery,
            safeQuery.toLowerCase(Locale.ROOT),
            pattern,
            safeQuery,
            Math.min(Math.max(limit, 1), 100));
    List<SearchHit> hits =
        results.stream()
            .map(
                item ->
                    new SearchHit(
                        item, List.of("body"), Map.of("body", List.of(item.bodyPreview()))))
            .toList();
    return new SearchResponse(safeQuery, hits, null, false);
  }

  private PreparedDocument prepare(
      JsonNode inputDocument, OwnerAccountEntity owner, UUID existingItemId) {
    validator.validate(inputDocument);
    JsonNode document = inputDocument.deepCopy();
    List<PreparedImage> preparedImages = new ArrayList<>();
    collectImages(document, owner, existingItemId, preparedImages);
    long totalBytes =
        preparedImages.stream().mapToLong(PreparedImage::sizeBytes).reduce(0L, Long::sum);
    if (totalBytes > MAX_TOTAL_IMAGE_BYTES) {
      throw new IllegalArgumentException("Processed inline images exceed 50 MiB.");
    }
    String plainText = projector.project(document);
    return new PreparedDocument(
        document, plainText, sha256(writeJson(document)), preparedImages, totalBytes);
  }

  private void collectImages(
      JsonNode node, OwnerAccountEntity owner, UUID existingItemId, List<PreparedImage> collected) {
    if (node == null) {
      return;
    }
    if (node.isObject() && "image".equals(node.path("type").asText())) {
      ObjectNode object = (ObjectNode) node;
      ObjectNode attrs = (ObjectNode) object.get("attrs");
      UUID assetId = UUID.fromString(attrs.path("assetId").asText());
      ResolvedAsset resolved =
          resolveAsset(assetId, owner, existingItemId, attrs.path("nodeId").asText());
      attrs.put("assetId", resolved.attachmentId().toString());
      attrs.remove("uploadId");
      collected.add(
          new PreparedImage(
              attrs.path("nodeId").asText(),
              resolved.attachmentId(),
              attrs.path("alt").asText(""),
              attrs.path("caption").asText(""),
              attrs.path("displayWidthPx").asInt(),
              attrs.path("alignment").asText(),
              attrs.path("decorative").asBoolean(false),
              resolved.sizeBytes()));
    }
    if (node.isContainerNode()) {
      node.forEach(child -> collectImages(child, owner, existingItemId, collected));
    }
  }

  private ResolvedAsset resolveAsset(
      UUID assetId, OwnerAccountEntity owner, UUID itemId, String nodeId) {
    var existing = attachments.findByIdAndOwnerId(assetId, owner.getId());
    if (existing.isPresent()) {
      AttachmentEntity attachment = existing.get();
      if (!"clean".equals(attachment.getScanState())) {
        throw new IllegalArgumentException("Inline image is not safe.");
      }
      return new ResolvedAsset(attachment.getId(), 0);
    }
    RichImageUploadEntity upload =
        uploads
            .findByIdAndOwnerId(assetId, owner.getId())
            .orElseThrow(
                () -> new IllegalArgumentException("Inline image upload was not found: " + nodeId));
    if (!"ready".equals(upload.getStatus())
        || !"clean".equals(upload.getScanState())
        || !List.of("none", "redacted").contains(upload.getRedactionState())) {
      throw new IllegalArgumentException("Inline image upload is not ready for saving.");
    }
    AttachmentEntity attachment =
        attachments.save(AttachmentEntity.fromUpload(owner.getId(), itemId, upload));
    upload.attach(itemId);
    uploads.save(upload);
    return new ResolvedAsset(attachment.getId(), upload.getSizeBytes());
  }

  private UUID insertRevision(
      KnowledgeItemEntity item, PreparedDocument prepared, String reason, int revisionNo) {
    UUID revisionId = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO rich_document_revision
          (id, item_id, revision_no, schema_version, document, plain_text,
           document_hash, reason, created_at)
        VALUES (?, ?, ?, 1, ?::jsonb, ?, ?, ?, ?)
        """,
        revisionId,
        item.getId(),
        revisionNo,
        writeJson(prepared.document()),
        prepared.plainText(),
        prepared.hash(),
        reason,
        Timestamp.from(Instant.now()));
    return revisionId;
  }

  private void insertImageReferences(
      KnowledgeItemEntity item, UUID revisionId, List<PreparedImage> images) {
    Timestamp now = Timestamp.from(Instant.now());
    for (PreparedImage image : images) {
      jdbc.update(
          """
          INSERT INTO rich_image_reference
            (id, item_id, revision_id, attachment_id, node_id, alt_text, caption,
             display_width_px, alignment, decorative, created_at, updated_at)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          """,
          UUID.randomUUID(),
          item.getId(),
          revisionId,
          image.attachmentId(),
          image.nodeId(),
          image.alt().isBlank() ? null : image.alt(),
          image.caption().isBlank() ? null : image.caption(),
          image.displayWidthPx(),
          image.alignment(),
          image.decorative(),
          now,
          now);
    }
  }

  private void insertSource(KnowledgeItemEntity item, BodySourceRequest input, String hash) {
    BodySourceRequest source = input == null ? new BodySourceRequest("native", null, null) : input;
    String kind = normalizeSourceKind(source.kind());
    String sourceUrl = sanitizeUrl(source.sourceUrl());
    String host = null;
    if (sourceUrl != null) {
      try {
        host = new URI(sourceUrl).getHost();
      } catch (URISyntaxException ignored) {
        throw new IllegalArgumentException("Source URL is invalid.");
      }
    }
    Instant now = Instant.now();
    Timestamp nowTimestamp = Timestamp.from(now);
    UUID sourceId = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO rich_document_source
          (id, item_id, source_kind, source_url, source_host, captured_at,
           converted_at, converter_version, content_hash, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        sourceId,
        item.getId(),
        kind,
        sourceUrl,
        host,
        source.capturedAt() == null ? null : Timestamp.from(source.capturedAt()),
        nowTimestamp,
        CONVERTER_VERSION,
        hash,
        nowTimestamp);
    jdbc.update(
        """
        INSERT INTO rich_document_conversion_report
          (id, source_id, preserved_features, downgraded_features,
           removed_features, rejected_features, converter_version, created_at)
        VALUES (?, ?, ARRAY[]::text[], ARRAY[]::text[], ARRAY[]::text[], ARRAY[]::text[], ?, ?)
        """,
        UUID.randomUUID(),
        sourceId,
        CONVERTER_VERSION,
        nowTimestamp);
  }

  private void updateSearch(KnowledgeItemEntity item, String plainText) {
    String searchable = (item.getTitle() + "\n" + plainText + "\n" + item.getNote()).strip();
    jdbc.update(
        """
        INSERT INTO search_document
          (item_id, owner_id, title_text, body_text, note_text, search_text,
           search_revision, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (item_id) DO UPDATE SET
          title_text = EXCLUDED.title_text,
          body_text = EXCLUDED.body_text,
          note_text = EXCLUDED.note_text,
          search_text = EXCLUDED.search_text,
          search_revision = EXCLUDED.search_revision,
          updated_at = EXCLUDED.updated_at
        """,
        item.getId(),
        item.getOwnerId(),
        item.getTitle(),
        plainText,
        item.getNote(),
        searchable,
        item.getSearchRevision(),
        Timestamp.from(Instant.now()));
  }

  private static KnowledgeItemResponse response(
      KnowledgeItemEntity item, RichDocumentEntity document) {
    return new KnowledgeItemResponse(
        item.getId(),
        item.getTitle(),
        item.getBody(),
        document.getDocument(),
        document.getSchemaVersion(),
        item.getBodyMigrationState(),
        item.getNote(),
        null,
        item.getProcessingStatus(),
        item.getLifecycleState(),
        false,
        new SourceSummary("manual", "manual", null, null),
        null,
        List.of(),
        item.getVersion(),
        item.getCreatedAt(),
        item.getUpdatedAt());
  }

  private static KnowledgeSummaryResponse summary(KnowledgeItemEntity item) {
    return new KnowledgeSummaryResponse(
        item.getId(),
        item.getTitle(),
        preview(item.getBody()),
        null,
        item.getProcessingStatus(),
        item.getLifecycleState(),
        new SourceSummary("manual", "manual", null, null),
        null,
        List.of(),
        item.getVersion(),
        item.getCreatedAt(),
        item.getUpdatedAt());
  }

  private static String preview(String text) {
    if (text == null) {
      return "";
    }
    String clean = text.replaceAll("\\s+", " ").strip();
    return clean.length() <= 220 ? clean : clean.substring(0, 217) + "...";
  }

  private static String normalizeSourceKind(String kind) {
    String value = kind == null ? "native" : kind.strip().toLowerCase(Locale.ROOT);
    if (!List.of(
            "native",
            "clipboard",
            "word",
            "google_docs",
            "webpage",
            "document",
            "screenshot",
            "legacy_text",
            "internet_result")
        .contains(value)) {
      throw new IllegalArgumentException("Unsupported body source kind.");
    }
    return value;
  }

  private static String sanitizeUrl(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      URI uri = new URI(raw.strip());
      if (!List.of("http", "https").contains(uri.getScheme())
          || uri.getHost() == null
          || uri.getUserInfo() != null) {
        throw new IllegalArgumentException("Source URL must be a public HTTP/HTTPS URL.");
      }
      return new URI(
              uri.getScheme(),
              null,
              uri.getHost(),
              uri.getPort(),
              uri.getPath(),
              uri.getQuery(),
              null)
          .toString();
    } catch (URISyntaxException error) {
      throw new IllegalArgumentException("Source URL is invalid.");
    }
  }

  private String writeJson(JsonNode document) {
    try {
      return mapper.writeValueAsString(document);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Rich text document could not be serialized.", error);
    }
  }

  private static String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 is unavailable.", error);
    }
  }

  private record PreparedDocument(
      JsonNode document,
      String plainText,
      String hash,
      List<PreparedImage> images,
      long totalImageBytes) {}

  private record PreparedImage(
      String nodeId,
      UUID attachmentId,
      String alt,
      String caption,
      int displayWidthPx,
      String alignment,
      boolean decorative,
      long sizeBytes) {}

  private record ResolvedAsset(UUID attachmentId, long sizeBytes) {}
}

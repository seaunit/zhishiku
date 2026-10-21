package site.seaunit.zhishiku.richtext.transaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import site.seaunit.zhishiku.richtext.provenance.NormalizedRichTextSource;
import site.seaunit.zhishiku.richtext.provenance.RichTextConversionReport;
import site.seaunit.zhishiku.search.richtext.SearchProjection;
import site.seaunit.zhishiku.search.richtext.SearchProjectionBuilder;

@Repository
public class JdbcRichTextTransactionWriter implements RichTextTransactionWriter {
  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;
  private final SearchProjectionBuilder searchProjections;

  public JdbcRichTextTransactionWriter(
      JdbcTemplate jdbc, ObjectMapper mapper, SearchProjectionBuilder searchProjections) {
    this.jdbc = jdbc;
    this.mapper = mapper;
    this.searchProjections = searchProjections;
  }

  @Override
  public long currentVersion(UUID itemId, UUID ownerId) {
    List<Long> versions =
        jdbc.query(
            """
            SELECT version
            FROM knowledge_item
            WHERE id = ? AND owner_id = ? AND lifecycle_state = 'active'
            """,
            (result, row) -> result.getLong("version"),
            itemId,
            ownerId);
    if (versions.isEmpty()) {
      throw new IllegalArgumentException("Knowledge item was not found.");
    }
    return versions.get(0);
  }

  @Override
  @Transactional
  public void commit(RichTextCommitCommand command) {
    LockedItem item = lockItem(command.itemId(), command.ownerId());
    if (item.version() != command.expectedVersion()) {
      throw new RichTextVersionConflictException(item.version());
    }
    Timestamp now = Timestamp.from(Instant.now());
    long nextVersion = item.version() + 1;
    RichTextSavePlan plan = command.plan();
    NormalizedRichTextSource source = command.source();
    String documentJson = writeJson(plan.document());

    jdbc.update(
        """
        INSERT INTO rich_document
          (item_id, schema_version, document, plain_text, document_hash,
           image_count, total_image_bytes, sanitized_at, version, created_at, updated_at)
        VALUES (?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (item_id) DO UPDATE SET
          schema_version = EXCLUDED.schema_version,
          document = EXCLUDED.document,
          plain_text = EXCLUDED.plain_text,
          document_hash = EXCLUDED.document_hash,
          image_count = EXCLUDED.image_count,
          total_image_bytes = EXCLUDED.total_image_bytes,
          sanitized_at = EXCLUDED.sanitized_at,
          version = EXCLUDED.version,
          updated_at = EXCLUDED.updated_at
        """,
        command.itemId(),
        plan.schemaVersion(),
        documentJson,
        plan.plainText(),
        plan.documentHash(),
        plan.imageCount(),
        plan.totalImageBytes(),
        now,
        nextVersion,
        now,
        now);

    insertSource(command.itemId(), source, now);
    int revisionNo =
        jdbc.queryForObject(
                "SELECT COALESCE(MAX(revision_no), 0) + 1 FROM rich_document_revision WHERE item_id = ?",
                Integer.class,
                command.itemId())
            .intValue();
    UUID revisionId = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO rich_document_revision
          (id, item_id, revision_no, schema_version, document, plain_text,
           document_hash, reason, source_id, created_at)
        VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, 'edit', ?, ?)
        """,
        revisionId,
        command.itemId(),
        revisionNo,
        plan.schemaVersion(),
        documentJson,
        plan.plainText(),
        plan.documentHash(),
        source.sourceId(),
        now);
    jdbc.update(
        """
        UPDATE rich_image_reference
        SET status = 'cleanup_pending', updated_at = ?
        WHERE item_id = ? AND status = 'active'
        """,
        now,
        command.itemId());
    insertImageReferences(command.itemId(), revisionId, plan.document(), now);

    SearchProjection projection =
        searchProjections.withRevision(
            searchProjections.project(item.title(), plan.document(), item.note()), nextVersion);
    jdbc.update(
        """
        INSERT INTO search_document
          (item_id, owner_id, title_text, body_text, note_text, image_text,
           search_text, search_revision, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (item_id) DO UPDATE SET
          title_text = EXCLUDED.title_text,
          body_text = EXCLUDED.body_text,
          note_text = EXCLUDED.note_text,
          image_text = EXCLUDED.image_text,
          search_text = EXCLUDED.search_text,
          search_revision = EXCLUDED.search_revision,
          updated_at = EXCLUDED.updated_at
        """,
        command.itemId(),
        command.ownerId(),
        projection.titleText(),
        projection.bodyText(),
        projection.noteText(),
        projection.imageText(),
        projection.searchText(),
        projection.expectedSearchRevision(),
        now);
    jdbc.update(
        """
        UPDATE knowledge_item
        SET body = ?,
            body_migration_state = 'completed',
            search_revision = ?,
            version = ?,
            updated_at = ?
        WHERE id = ? AND owner_id = ?
        """,
        plan.plainText(),
        nextVersion,
        nextVersion,
        now,
        command.itemId(),
        command.ownerId());
  }

  private LockedItem lockItem(UUID itemId, UUID ownerId) {
    List<LockedItem> items =
        jdbc.query(
            """
            SELECT title, note, version
            FROM knowledge_item
            WHERE id = ? AND owner_id = ? AND lifecycle_state = 'active'
            FOR UPDATE
            """,
            (result, row) ->
                new LockedItem(
                    result.getString("title"), result.getString("note"), result.getLong("version")),
            itemId,
            ownerId);
    if (items.isEmpty()) {
      throw new IllegalArgumentException("Knowledge item was not found.");
    }
    return items.get(0);
  }

  private void insertSource(UUID itemId, NormalizedRichTextSource source, Timestamp convertedAt) {
    jdbc.update(
        """
        INSERT INTO rich_document_source
          (id, item_id, source_kind, source_url, source_host, captured_at,
           converted_at, converter_version, content_hash, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        source.sourceId(),
        itemId,
        source.kind(),
        source.url(),
        source.host(),
        source.capturedAt() == null ? null : Timestamp.from(source.capturedAt()),
        convertedAt,
        source.converterVersion(),
        source.contentHash(),
        convertedAt);
    RichTextConversionReport report = source.report();
    jdbc.update(
        """
        INSERT INTO rich_document_conversion_report
          (id, source_id, preserved_features, downgraded_features,
           removed_features, rejected_features, converter_version, created_at)
        VALUES (?, ?, ?::text[], ?::text[], ?::text[], ?::text[], ?, ?)
        """,
        UUID.randomUUID(),
        source.sourceId(),
        textArray(report.preservedFeatures()),
        textArray(report.downgradedFeatures()),
        textArray(report.removedFeatures()),
        textArray(report.rejectedFeatures()),
        report.converterVersion(),
        convertedAt);
  }

  private void insertImageReferences(
      UUID itemId, UUID revisionId, JsonNode document, Timestamp now) {
    List<ImageReference> images = new ArrayList<>();
    collectImages(document, images);
    for (ImageReference image : images) {
      jdbc.update(
          """
          INSERT INTO rich_image_reference
            (id, item_id, revision_id, attachment_id, node_id, alt_text, caption,
             display_width_px, alignment, decorative, status, created_at, updated_at)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'active', ?, ?)
          """,
          UUID.randomUUID(),
          itemId,
          revisionId,
          image.assetId(),
          image.nodeId(),
          image.alt(),
          image.caption(),
          image.displayWidthPx(),
          image.alignment(),
          image.decorative(),
          now,
          now);
    }
  }

  private static void collectImages(JsonNode node, List<ImageReference> images) {
    if (node == null) {
      return;
    }
    if (node.isObject() && "image".equals(node.path("type").asText())) {
      JsonNode attrs = node.path("attrs");
      images.add(
          new ImageReference(
              UUID.fromString(attrs.path("assetId").asText()),
              attrs.path("nodeId").asText(),
              blankToNull(attrs.path("alt").asText("")),
              blankToNull(attrs.path("caption").asText("")),
              attrs.path("displayWidthPx").asInt(),
              attrs.path("alignment").asText(),
              attrs.path("decorative").asBoolean(false)));
    }
    if (node.isContainerNode()) {
      node.forEach(child -> collectImages(child, images));
    }
  }

  private String writeJson(JsonNode document) {
    try {
      return mapper.writeValueAsString(document);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Rich text document could not be serialized.", error);
    }
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private static String textArray(List<String> values) {
    return "{"
        + values.stream()
            .map(value -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
            .collect(java.util.stream.Collectors.joining(","))
        + "}";
  }

  private record LockedItem(String title, String note, long version) {}

  private record ImageReference(
      UUID assetId,
      String nodeId,
      String alt,
      String caption,
      int displayWidthPx,
      String alignment,
      boolean decorative) {}
}

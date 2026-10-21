package site.seaunit.zhishiku.richtext.migration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import site.seaunit.zhishiku.richtext.provenance.SourceProvenancePolicy;
import site.seaunit.zhishiku.search.richtext.SearchProjection;
import site.seaunit.zhishiku.search.richtext.SearchProjectionBuilder;

@Service
public class LegacyTextMigrationService {
  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;
  private final LegacyTextDocumentConverter converter;
  private final SearchProjectionBuilder searchProjectionBuilder;
  private final TransactionTemplate transactionTemplate;

  public LegacyTextMigrationService(
      JdbcTemplate jdbc,
      ObjectMapper mapper,
      LegacyTextDocumentConverter converter,
      SearchProjectionBuilder searchProjectionBuilder,
      PlatformTransactionManager transactionManager) {
    this.jdbc = jdbc;
    this.mapper = mapper;
    this.converter = converter;
    this.searchProjectionBuilder = searchProjectionBuilder;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  @Transactional
  public int enqueueMissing(int limit) {
    int boundedLimit = Math.min(Math.max(limit, 1), 500);
    jdbc.update(
        """
        UPDATE rich_document_migration
        SET status = 'pending', updated_at = now()
        WHERE status = 'running'
          AND started_at < now() - INTERVAL '15 minutes'
        """);
    List<PendingSource> sources =
        jdbc.query(
            """
            SELECT k.id, k.body
            FROM knowledge_item k
            LEFT JOIN rich_document d ON d.item_id = k.id
            LEFT JOIN rich_document_migration m ON m.item_id = k.id
            WHERE d.item_id IS NULL
              AND m.item_id IS NULL
              AND k.lifecycle_state = 'active'
            ORDER BY k.created_at
            LIMIT ?
            """,
            (result, row) ->
                new PendingSource(result.getObject("id", UUID.class), result.getString("body")),
            boundedLimit);
    Timestamp now = Timestamp.from(Instant.now());
    for (PendingSource source : sources) {
      LegacyTextDocumentConverter.Conversion conversion = converter.convert(source.body());
      jdbc.update(
          """
          INSERT INTO rich_document_migration
            (item_id, status, attempt_count, source_hash, error_code,
             started_at, finished_at, updated_at)
          VALUES (?, 'pending', 0, ?, NULL, NULL, NULL, ?)
          ON CONFLICT (item_id) DO NOTHING
          """,
          source.itemId(),
          conversion.sourceHash(),
          now);
      jdbc.update(
          """
          UPDATE knowledge_item
          SET body_migration_state = 'pending', updated_at = ?
          WHERE id = ? AND body_migration_state <> 'pending'
          """,
          now,
          source.itemId());
    }
    return sources.size();
  }

  public MigrationSummary migratePending(int limit) {
    enqueueMissing(limit);
    List<UUID> pending =
        jdbc.query(
            """
            SELECT item_id
            FROM rich_document_migration
            WHERE status IN ('pending', 'failed')
            ORDER BY updated_at
            LIMIT ?
            """,
            (result, row) -> result.getObject("item_id", UUID.class),
            Math.min(Math.max(limit, 1), 200));
    int migrated = 0;
    int failed = 0;
    for (UUID itemId : pending) {
      if (migrate(itemId)) {
        migrated++;
      } else {
        failed++;
      }
    }
    return new MigrationSummary(pending.size(), migrated, failed);
  }

  public boolean migrate(UUID itemId) {
    try {
      return Boolean.TRUE.equals(
          transactionTemplate.execute(status -> migrateInTransaction(itemId)));
    } catch (RuntimeException error) {
      recordFailure(itemId);
      return false;
    }
  }

  private boolean migrateInTransaction(UUID itemId) {
    List<MigrationRow> rows =
        jdbc.query(
            """
            SELECT m.status, m.source_hash, k.title, k.body, k.version
            FROM rich_document_migration m
            JOIN knowledge_item k ON k.id = m.item_id
            WHERE m.item_id = ?
              AND k.lifecycle_state = 'active'
            FOR UPDATE OF m, k
            """,
            (result, row) ->
                new MigrationRow(
                    result.getString("status"),
                    result.getString("source_hash"),
                    result.getString("title"),
                    result.getString("body"),
                    result.getLong("version")),
            itemId);
    if (rows.isEmpty()) {
      return false;
    }
    MigrationRow row = rows.get(0);
    Instant now = Instant.now();
    Timestamp timestamp = Timestamp.from(now);
    if ("migrated".equals(row.status())) {
      return true;
    }
    if (documentExists(itemId)) {
      markMigrated(itemId, timestamp);
      return true;
    }

    jdbc.update(
        """
        UPDATE rich_document_migration
        SET status = 'running',
            attempt_count = attempt_count + 1,
            started_at = ?,
            error_code = NULL,
            updated_at = ?
        WHERE item_id = ?
        """,
        timestamp,
        timestamp,
        itemId);

    LegacyTextDocumentConverter.Conversion conversion = converter.convert(row.body());
    jdbc.update(
        "UPDATE rich_document_migration SET source_hash = ? WHERE item_id = ?",
        conversion.sourceHash(),
        itemId);
    jdbc.update(
        """
        INSERT INTO rich_document
          (item_id, schema_version, document, plain_text, document_hash,
           image_count, total_image_bytes, sanitized_at, version, created_at, updated_at)
        VALUES (?, 1, ?::jsonb, ?, ?, 0, 0, ?, ?, ?, ?)
        """,
        itemId,
        writeJson(conversion.document()),
        conversion.plainText(),
        conversion.sourceHash(),
        timestamp,
        row.version() + 1,
        timestamp,
        timestamp);

    int revisionNo =
        jdbc.queryForObject(
                "SELECT COALESCE(MAX(revision_no), 0) + 1 FROM rich_document_revision WHERE item_id = ?",
                Integer.class,
                itemId)
            .intValue();
    UUID revisionId = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO rich_document_revision
          (id, item_id, revision_no, schema_version, document, plain_text,
           document_hash, reason, created_at)
        VALUES (?, ?, ?, 1, ?::jsonb, ?, ?, 'migration', ?)
        """,
        revisionId,
        itemId,
        revisionNo,
        writeJson(conversion.document()),
        conversion.plainText(),
        conversion.sourceHash(),
        timestamp);

    UUID sourceId = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO rich_document_source
          (id, item_id, source_kind, source_url, source_host, captured_at,
           converted_at, converter_version, content_hash, created_at)
        VALUES (?, ?, 'legacy_text', NULL, NULL, ?, ?, ?, ?, ?)
        """,
        sourceId,
        itemId,
        timestamp,
        timestamp,
        SourceProvenancePolicy.CONVERTER_VERSION,
        conversion.sourceHash(),
        timestamp);
    jdbc.update(
        """
        INSERT INTO rich_document_conversion_report
          (id, source_id, preserved_features, downgraded_features,
           removed_features, rejected_features, converter_version, created_at)
        VALUES (?, ?, ARRAY['paragraph','hardBreak','link']::text[],
                ARRAY[]::text[], ARRAY[]::text[], ARRAY[]::text[], ?, ?)
        """,
        UUID.randomUUID(),
        sourceId,
        SourceProvenancePolicy.CONVERTER_VERSION,
        timestamp);

    long nextVersion = row.version() + 1;
    SearchProjection projection =
        searchProjectionBuilder.withRevision(
            searchProjectionBuilder.project(row.title(), conversion.document(), ""), nextVersion);
    jdbc.update(
        """
        INSERT INTO search_document
          (item_id, owner_id, title_text, body_text, note_text, image_text,
           search_text, search_revision, updated_at)
        SELECT ?, k.owner_id, ?, ?, k.note, ?, ?, ?, ?
        FROM knowledge_item k
        WHERE k.id = ?
        ON CONFLICT (item_id) DO UPDATE SET
          title_text = EXCLUDED.title_text,
          body_text = EXCLUDED.body_text,
          note_text = EXCLUDED.note_text,
          image_text = EXCLUDED.image_text,
          search_text = EXCLUDED.search_text,
          search_revision = EXCLUDED.search_revision,
          updated_at = EXCLUDED.updated_at
        """,
        itemId,
        projection.titleText(),
        projection.bodyText(),
        projection.imageText(),
        projection.searchText(),
        projection.expectedSearchRevision(),
        timestamp,
        itemId);
    jdbc.update(
        """
        UPDATE knowledge_item
        SET body = ?,
            body_migration_state = 'completed',
            search_revision = ?,
            version = ?,
            updated_at = ?
        WHERE id = ?
        """,
        conversion.plainText(),
        nextVersion,
        nextVersion,
        timestamp,
        itemId);
    markMigrated(itemId, timestamp);
    return true;
  }

  private void markMigrated(UUID itemId, Timestamp timestamp) {
    jdbc.update(
        """
        UPDATE rich_document_migration
        SET status = 'migrated',
            error_code = NULL,
            finished_at = ?,
            updated_at = ?
        WHERE item_id = ?
        """,
        timestamp,
        timestamp,
        itemId);
    jdbc.update(
        """
        UPDATE knowledge_item
        SET body = COALESCE(
                (SELECT d.plain_text FROM rich_document d WHERE d.item_id = knowledge_item.id),
                body
            ),
            body_migration_state = 'completed',
            updated_at = ?
        WHERE id = ?
        """,
        timestamp,
        itemId);
  }

  private boolean documentExists(UUID itemId) {
    Integer count =
        jdbc.queryForObject(
            "SELECT count(*) FROM rich_document WHERE item_id = ?", Integer.class, itemId);
    return count != null && count > 0;
  }

  private void recordFailure(UUID itemId) {
    try {
      transactionTemplate.executeWithoutResult(
          status -> {
            Timestamp now = Timestamp.from(Instant.now());
            jdbc.update(
                """
                UPDATE rich_document_migration
                SET status = 'failed',
                    error_code = 'MIGRATION_FAILED',
                    finished_at = ?,
                    updated_at = ?
                WHERE item_id = ?
                """,
                now,
                now,
                itemId);
            jdbc.update(
                """
                UPDATE knowledge_item
                SET body_migration_state = 'failed', updated_at = ?
                WHERE id = ?
                """,
                now,
                itemId);
          });
    } catch (RuntimeException ignored) {
      // The original migration failure remains the safe, user-visible outcome.
    }
  }

  private String writeJson(com.fasterxml.jackson.databind.JsonNode document) {
    try {
      return mapper.writeValueAsString(document);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Migrated rich text could not be serialized.", error);
    }
  }

  public record MigrationSummary(int inspected, int migrated, int failed) {}

  private record PendingSource(UUID itemId, String body) {}

  private record MigrationRow(
      String status, String sourceHash, String title, String body, long version) {}
}

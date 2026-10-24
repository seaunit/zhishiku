package site.seaunit.zhishiku.richtext.provenance;

import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import site.seaunit.zhishiku.knowledge.SourceUrlCanonicalizer;

@Repository
public class JdbcRichTextProvenanceRepository implements RichTextProvenanceRepository {
  private final JdbcTemplate jdbc;
  private final SourceUrlCanonicalizer canonicalizer;

  public JdbcRichTextProvenanceRepository(
      JdbcTemplate jdbc, SourceUrlCanonicalizer canonicalizer) {
    this.jdbc = jdbc;
    this.canonicalizer = canonicalizer;
  }

  @Override
  public boolean ownsItem(UUID itemId, UUID ownerId) {
    Integer count =
        jdbc.queryForObject(
            "SELECT count(*) FROM knowledge_item WHERE id = ? AND owner_id = ?",
            Integer.class,
            itemId,
            ownerId);
    return count != null && count > 0;
  }

  @Override
  public void insertSource(UUID itemId, NormalizedRichTextSource source) {
    jdbc.update(
        """
        INSERT INTO rich_document_source
          (id, item_id, source_kind, source_url, source_host, canonical_url,
           captured_at, converted_at, converter_version, content_hash, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        source.sourceId(),
        itemId,
        source.kind(),
        source.url(),
        source.host(),
        source.url() == null ? null : canonicalizer.canonicalize(source.url()),
        source.capturedAt() == null ? null : Timestamp.from(source.capturedAt()),
        Timestamp.from(source.convertedAt()),
        source.converterVersion(),
        source.contentHash(),
        Timestamp.from(source.convertedAt()));
  }

  @Override
  public void insertConversionReport(NormalizedRichTextSource source) {
    RichTextConversionReport report = source.report();
    jdbc.update(
        connection -> {
          var statement =
              connection.prepareStatement(
                  """
                  INSERT INTO rich_document_conversion_report
                    (id, source_id, preserved_features, downgraded_features,
                     removed_features, rejected_features, converter_version, created_at)
                  VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                  """);
          statement.setObject(1, UUID.randomUUID());
          statement.setObject(2, source.sourceId());
          statement.setArray(
              3, connection.createArrayOf("text", report.preservedFeatures().toArray()));
          statement.setArray(
              4, connection.createArrayOf("text", report.downgradedFeatures().toArray()));
          statement.setArray(
              5, connection.createArrayOf("text", report.removedFeatures().toArray()));
          statement.setArray(
              6, connection.createArrayOf("text", report.rejectedFeatures().toArray()));
          statement.setString(7, report.converterVersion());
          statement.setTimestamp(8, Timestamp.from(source.convertedAt()));
          return statement;
        });
  }
}

package site.seaunit.zhishiku.knowledge;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeDuplicateService {
  private final JdbcTemplate jdbc;
  private final SourceUrlCanonicalizer canonicalizer;

  public KnowledgeDuplicateService(
      JdbcTemplate jdbc, SourceUrlCanonicalizer canonicalizer) {
    this.jdbc = jdbc;
    this.canonicalizer = canonicalizer;
  }

  public void lockSource(UUID ownerId, String rawUrl) {
    String canonicalUrl;
    try {
      canonicalUrl = canonicalizer.canonicalize(rawUrl);
    } catch (IllegalArgumentException error) {
      return;
    }
    jdbc.query(
        "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
        resultSet -> {},
        ownerId + "\n" + canonicalUrl);
  }

  public List<DuplicateKnowledge> findActiveBySourceUrl(UUID ownerId, String rawUrl) {
    String canonicalUrl;
    try {
      canonicalUrl = canonicalizer.canonicalize(rawUrl);
    } catch (IllegalArgumentException error) {
      return List.of();
    }
    return jdbc.query(
        """
        SELECT k.id, k.title
        FROM knowledge_item k
        JOIN rich_document_source s ON s.item_id = k.id
        WHERE k.owner_id = ?
          AND k.lifecycle_state = 'active'
          AND s.source_kind IN ('webpage', 'internet_result')
          AND (s.canonical_url = ? OR (s.canonical_url IS NULL AND s.source_url = ?))
        GROUP BY k.id, k.title, k.created_at
        ORDER BY k.created_at, k.id
        LIMIT 20
        """,
        (rs, row) -> new DuplicateKnowledge(rs.getObject("id", UUID.class), rs.getString("title")),
        ownerId,
        canonicalUrl,
        rawUrl == null ? null : rawUrl.strip());
  }
}

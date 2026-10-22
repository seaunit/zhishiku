package site.seaunit.zhishiku.knowledge;

import java.sql.Timestamp;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.ClassificationSummary;

@Service
public class ClassificationService {
  private static final int MAX_TAGS = 8;

  private final JdbcTemplate jdbc;

  public ClassificationService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional
  public void assignAi(
      UUID ownerId, UUID itemId, String category, List<String> tags, double confidence) {
    List<String> normalizedTags = normalizeTags(tags);
    Timestamp now = Timestamp.from(Instant.now());
    jdbc.update(
        """
        DELETE FROM knowledge_classification_assignment
        WHERE item_id = ? AND origin = 'ai' AND assignment_status <> 'locked'
        """,
        itemId);
    String normalizedCategory = normalizeName(category);
    if (!normalizedCategory.isBlank()) {
      assign(
          ownerId,
          itemId,
          "category",
          normalizedCategory,
          clampedConfidence(confidence) >= 0.75 ? "applied" : "provisional",
          "ai",
          clampedConfidence(confidence),
          now);
    }
    for (String tag : normalizedTags) {
      assign(ownerId, itemId, "tag", tag, "applied", "ai", clampedConfidence(confidence), now);
    }
  }

  @Transactional
  public void replaceUser(UUID ownerId, UUID itemId, String category, List<String> tags) {
    String normalizedCategory = normalizeName(category);
    if (normalizedCategory.isBlank()) {
      throw new IllegalArgumentException("Category must not be blank.");
    }
    List<String> normalizedTags = normalizeTags(tags);
    jdbc.update("DELETE FROM knowledge_classification_assignment WHERE item_id = ?", itemId);
    Timestamp now = Timestamp.from(Instant.now());
    assign(ownerId, itemId, "category", normalizedCategory, "locked", "user", null, now);
    for (String tag : normalizedTags) {
      assign(ownerId, itemId, "tag", tag, "locked", "user", null, now);
    }
  }

  @Transactional(readOnly = true)
  public ClassificationSet read(UUID itemId) {
    ClassificationSummary category = null;
    List<ClassificationSummary> tags = new ArrayList<>();
    List<AssignmentRow> rows =
        jdbc.query(
            """
            SELECT t.id, t.name, a.kind, a.assignment_status, a.origin,
                   a.confidence, a.locked_at
            FROM knowledge_classification_assignment a
            JOIN knowledge_classification_term t ON t.id = a.term_id
            WHERE a.item_id = ?
            ORDER BY a.kind, t.name
            """,
            (rs, row) ->
                new AssignmentRow(
                    rs.getString("kind"),
                    new ClassificationSummary(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("assignment_status"),
                        rs.getString("origin"),
                        rs.getObject("confidence") == null ? null : rs.getDouble("confidence"),
                        rs.getTimestamp("locked_at") == null
                            ? null
                            : rs.getTimestamp("locked_at").toInstant())),
            itemId);
    for (AssignmentRow row : rows) {
      if ("category".equals(row.kind())) {
        category = row.value();
      } else {
        tags.add(row.value());
      }
    }
    return new ClassificationSet(category, List.copyOf(tags));
  }

  @Transactional(readOnly = true)
  public Map<UUID, ClassificationSet> readAll(Collection<UUID> itemIds) {
    Map<UUID, ClassificationSet> values = new HashMap<>();
    for (UUID itemId : itemIds) {
      values.put(itemId, read(itemId));
    }
    return values;
  }

  static String normalizeName(String value) {
    if (value == null) {
      return "";
    }
    String normalized =
        Normalizer.normalize(value, Normalizer.Form.NFKC).strip().replaceAll("\\s+", " ");
    if (normalized.length() > 100) {
      throw new IllegalArgumentException("Classification names must not exceed 100 characters.");
    }
    return normalized;
  }

  private List<String> normalizeTags(List<String> tags) {
    if (tags == null) {
      return List.of();
    }
    Set<String> result = new LinkedHashSet<>();
    for (String tag : tags) {
      String normalized = normalizeName(tag);
      if (!normalized.isBlank()) {
        result.add(normalized);
      }
    }
    if (result.size() > MAX_TAGS) {
      throw new IllegalArgumentException("At most eight tags are allowed.");
    }
    return List.copyOf(result);
  }

  private void assign(
      UUID ownerId,
      UUID itemId,
      String kind,
      String name,
      String status,
      String origin,
      Double confidence,
      Timestamp now) {
    UUID termId =
        jdbc.queryForObject(
            """
            INSERT INTO knowledge_classification_term
              (id, owner_id, kind, name, normalized_name, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, 'active', ?, ?)
            ON CONFLICT (owner_id, kind, normalized_name)
            DO UPDATE SET updated_at = EXCLUDED.updated_at
            RETURNING id
            """,
            UUID.class,
            UUID.randomUUID(),
            ownerId,
            kind,
            name,
            name.toLowerCase(Locale.ROOT),
            now,
            now);
    jdbc.update(
        """
        INSERT INTO knowledge_classification_assignment
          (item_id, term_id, kind, assignment_status, origin, confidence,
           locked_at, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (item_id, term_id) DO NOTHING
        """,
        itemId,
        termId,
        kind,
        status,
        origin,
        confidence,
        "locked".equals(status) ? now : null,
        now,
        now);
  }

  private static double clampedConfidence(double confidence) {
    return Math.max(0, Math.min(1, confidence));
  }

  public record ClassificationSet(
      ClassificationSummary category, List<ClassificationSummary> tags) {
    public static ClassificationSet empty() {
      return new ClassificationSet(null, List.of());
    }
  }

  private record AssignmentRow(String kind, ClassificationSummary value) {}
}

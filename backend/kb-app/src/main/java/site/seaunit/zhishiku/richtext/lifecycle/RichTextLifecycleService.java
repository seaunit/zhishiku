package site.seaunit.zhishiku.richtext.lifecycle;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RichTextLifecycleService {
  private final JdbcTemplate jdbc;
  private final RichTextLifecyclePolicy policy;

  public RichTextLifecycleService(JdbcTemplate jdbc, RichTextLifecyclePolicy policy) {
    this.jdbc = jdbc;
    this.policy = policy;
  }

  @Transactional
  public LifecycleResult recycle(UUID ownerId, UUID itemId) {
    return transition(ownerId, itemId, "recycle");
  }

  @Transactional
  public LifecycleResult restore(UUID ownerId, UUID itemId) {
    return transition(ownerId, itemId, "restore");
  }

  @Transactional
  public LifecycleResult purge(UUID ownerId, UUID itemId) {
    String current = lockLifecycleState(ownerId, itemId);
    String purging = policy.next(current, "purge");
    Timestamp now = Timestamp.from(Instant.now());
    jdbc.update(
        "UPDATE knowledge_item SET lifecycle_state = ?, updated_at = ? WHERE id = ? AND owner_id = ?",
        purging,
        now,
        itemId,
        ownerId);
    jdbc.update(
        """
        INSERT INTO attachment_cleanup
          (attachment_id, owner_id, storage_key, reason, not_before, created_at)
        SELECT a.id, a.owner_id, a.storage_key, 'item_purged', ?, ?
        FROM attachment a
        WHERE a.item_id = ?
          AND NOT EXISTS (
            SELECT 1
            FROM rich_image_reference r
            WHERE r.attachment_id = a.id
              AND r.item_id <> ?
              AND r.status IN ('active', 'cleanup_pending')
          )
        ON CONFLICT (attachment_id) DO NOTHING
        """,
        now,
        now,
        itemId,
        itemId);
    jdbc.update(
        "UPDATE attachment SET item_id = NULL, updated_at = ? WHERE item_id = ?", now, itemId);
    jdbc.update(
        "DELETE FROM knowledge_item WHERE id = ? AND owner_id = ? AND lifecycle_state = 'purging'",
        itemId,
        ownerId);
    return new LifecycleResult(itemId, "purged");
  }

  public int markSupersededReferences(UUID itemId, UUID currentRevisionId) {
    return jdbc.update(
        """
        UPDATE rich_image_reference
        SET status = 'cleanup_pending', updated_at = ?
        WHERE item_id = ?
          AND revision_id <> ?
          AND status = 'active'
        """,
        Timestamp.from(Instant.now()),
        itemId,
        currentRevisionId);
  }

  public List<UUID> cleanupQueue(int limit) {
    return jdbc.query(
        """
        SELECT attachment_id
        FROM attachment_cleanup
        WHERE not_before <= now()
        ORDER BY not_before
        LIMIT ?
        """,
        (result, row) -> result.getObject("attachment_id", UUID.class),
        Math.min(Math.max(limit, 1), 200));
  }

  public int enqueueOrphanAttachments(UUID ownerId) {
    Timestamp now = Timestamp.from(Instant.now());
    return jdbc.update(
        """
        INSERT INTO attachment_cleanup
          (attachment_id, owner_id, storage_key, reason, not_before, created_at)
        SELECT a.id, a.owner_id, a.storage_key, 'reference_removed', ?, ?
        FROM attachment a
        WHERE a.owner_id = ?
          AND a.item_id IS NULL
          AND NOT EXISTS (
            SELECT 1 FROM rich_image_reference r WHERE r.attachment_id = a.id
          )
        ON CONFLICT (attachment_id) DO NOTHING
        """,
        now,
        now,
        ownerId);
  }

  private LifecycleResult transition(UUID ownerId, UUID itemId, String action) {
    String current = lockLifecycleState(ownerId, itemId);
    String next = policy.next(current, action);
    jdbc.update(
        "UPDATE knowledge_item SET lifecycle_state = ?, updated_at = ? WHERE id = ? AND owner_id = ?",
        next,
        Timestamp.from(Instant.now()),
        itemId,
        ownerId);
    return new LifecycleResult(itemId, next);
  }

  private String lockLifecycleState(UUID ownerId, UUID itemId) {
    List<String> states =
        jdbc.query(
            """
            SELECT lifecycle_state
            FROM knowledge_item
            WHERE id = ? AND owner_id = ?
            FOR UPDATE
            """,
            (result, row) -> result.getString("lifecycle_state"),
            itemId,
            ownerId);
    if (states.isEmpty()) {
      throw new IllegalArgumentException("Knowledge item was not found.");
    }
    return states.get(0);
  }

  public record LifecycleResult(UUID itemId, String lifecycleState) {}
}

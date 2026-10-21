package site.seaunit.zhishiku.richtext.image;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcRichTextItemAccess implements RichTextItemAccess {
  private final JdbcTemplate jdbc;

  public JdbcRichTextItemAccess(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void requireWritableItem(UUID itemId, UUID ownerId) {
    if (itemId == null) {
      return;
    }
    Integer count =
        jdbc.queryForObject(
            """
            SELECT count(*)
            FROM knowledge_item
            WHERE id = ? AND owner_id = ? AND lifecycle_state = 'active'
            """,
            Integer.class,
            itemId,
            ownerId);
    if (count == null || count == 0) {
      throw new IllegalArgumentException("Knowledge item was not found.");
    }
  }
}

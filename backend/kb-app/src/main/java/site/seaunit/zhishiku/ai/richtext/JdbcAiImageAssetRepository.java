package site.seaunit.zhishiku.ai.richtext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAiImageAssetRepository implements AiImageAssetRepository {
  private final JdbcTemplate jdbc;

  public JdbcAiImageAssetRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<AiImageAsset> findOwnedSafeAsset(UUID ownerId, UUID assetId) {
    List<AiImageAsset> attachments =
        jdbc.query(
            """
            SELECT id, sha256 AS content_hash
            FROM attachment
            WHERE id = ?
              AND owner_id = ?
              AND scan_state = 'clean'
              AND redaction_state IN ('none', 'not_required', 'redacted')
            """,
            (result, row) ->
                new AiImageAsset(
                    result.getObject("id", UUID.class), result.getString("content_hash")),
            assetId,
            ownerId);
    if (!attachments.isEmpty()) {
      return Optional.of(attachments.get(0));
    }
    List<AiImageAsset> uploads =
        jdbc.query(
            """
            SELECT id, content_hash
            FROM rich_image_upload
            WHERE id = ?
              AND owner_id = ?
              AND status = 'ready'
              AND scan_state = 'clean'
              AND redaction_state IN ('none', 'redacted')
            """,
            (result, row) ->
                new AiImageAsset(
                    result.getObject("id", UUID.class), result.getString("content_hash")),
            assetId,
            ownerId);
    return uploads.stream().findFirst();
  }
}

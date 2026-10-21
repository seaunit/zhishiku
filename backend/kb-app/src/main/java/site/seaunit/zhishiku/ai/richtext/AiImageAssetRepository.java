package site.seaunit.zhishiku.ai.richtext;

import java.util.Optional;
import java.util.UUID;

public interface AiImageAssetRepository {
  Optional<AiImageAsset> findOwnedSafeAsset(UUID ownerId, UUID assetId);
}

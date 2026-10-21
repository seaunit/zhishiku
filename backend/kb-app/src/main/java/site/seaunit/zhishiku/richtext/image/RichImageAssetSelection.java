package site.seaunit.zhishiku.richtext.image;

import java.util.List;
import java.util.UUID;

public record RichImageAssetSelection(List<UUID> imageAssetIds, List<String> contentHashes) {
  public RichImageAssetSelection {
    imageAssetIds = List.copyOf(imageAssetIds);
    contentHashes = List.copyOf(contentHashes);
  }
}

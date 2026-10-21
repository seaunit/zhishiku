package site.seaunit.zhishiku.core.richtext;

import java.util.List;
import java.util.UUID;

public record RichTextValidation(
    int schemaVersion, int imageCount, List<RichTextImageAsset> images, List<UUID> uniqueAssetIds) {

  public RichTextValidation {
    images = List.copyOf(images);
    uniqueAssetIds = List.copyOf(uniqueAssetIds);
  }
}

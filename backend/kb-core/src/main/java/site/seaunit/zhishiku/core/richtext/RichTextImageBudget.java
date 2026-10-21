package site.seaunit.zhishiku.core.richtext;

import java.util.Map;
import java.util.UUID;

public final class RichTextImageBudget {
  private RichTextImageBudget() {}

  public static long validate(
      RichTextValidation validation, Map<UUID, Long> processedSizeByAssetId) {
    long total = 0;
    for (UUID assetId : validation.uniqueAssetIds()) {
      Long size = processedSizeByAssetId.get(assetId);
      if (size == null) {
        throw new RichTextException(
            "IMAGE_METADATA_MISSING", "Processed image metadata is missing for an inline image.");
      }
      if (size <= 0) {
        throw new RichTextException(
            "IMAGE_METADATA_INVALID", "Processed image size must be positive.");
      }
      if (Long.MAX_VALUE - total < size) {
        throw new RichTextException("IMAGE_TOTAL_BYTES", "Processed inline images exceed 50 MiB.");
      }
      total += size;
    }
    if (total > RichTextLimits.defaults().maxImageBytes()) {
      throw new RichTextException("IMAGE_TOTAL_BYTES", "Processed inline images exceed 50 MiB.");
    }
    return total;
  }
}

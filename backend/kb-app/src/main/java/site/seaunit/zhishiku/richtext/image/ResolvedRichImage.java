package site.seaunit.zhishiku.richtext.image;

import java.util.UUID;

public record ResolvedRichImage(
    UUID assetId, String contentHash, long sizeBytes, String mediaType, int width, int height) {}

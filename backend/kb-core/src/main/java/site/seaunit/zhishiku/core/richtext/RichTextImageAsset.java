package site.seaunit.zhishiku.core.richtext;

import java.util.UUID;

public record RichTextImageAsset(
    UUID assetId,
    String nodeId,
    String alt,
    String caption,
    int displayWidthPx,
    double aspectRatio,
    String alignment,
    boolean decorative) {}

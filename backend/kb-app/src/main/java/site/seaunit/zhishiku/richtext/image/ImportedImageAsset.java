package site.seaunit.zhishiku.richtext.image;

import java.util.UUID;

public record ImportedImageAsset(
    UUID uploadId, String alt, String caption, int width, int height, double aspectRatio) {}

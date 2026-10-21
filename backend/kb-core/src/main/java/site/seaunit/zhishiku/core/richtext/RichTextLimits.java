package site.seaunit.zhishiku.core.richtext;

public record RichTextLimits(
    int maxBlocks,
    int maxDepth,
    int maxCharacters,
    int maxImages,
    long maxImageBytes,
    int maxDisplayWidth) {

  public static RichTextLimits defaults() {
    return new RichTextLimits(5_000, 32, 200_000, 20, 50L * 1024 * 1024, 1_600);
  }
}

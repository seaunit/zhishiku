package site.seaunit.zhishiku.richtext.transaction;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import site.seaunit.zhishiku.core.richtext.RichTextImageAsset;

public record RichTextSavePlan(
    int schemaVersion,
    JsonNode document,
    String plainText,
    String documentHash,
    int imageCount,
    long totalImageBytes,
    List<RichTextImageAsset> images) {
  public RichTextSavePlan {
    document = document.deepCopy();
    images = List.copyOf(images);
  }

  @Override
  public JsonNode document() {
    return document.deepCopy();
  }
}

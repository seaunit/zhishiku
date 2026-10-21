package site.seaunit.zhishiku.core.richtext;

import com.fasterxml.jackson.databind.JsonNode;

public record RichTextDocumentPayload(int schemaVersion, JsonNode document) {
  public RichTextDocumentPayload {
    if (schemaVersion < 1 || schemaVersion > RichTextSchema.CURRENT_VERSION) {
      throw new IllegalArgumentException("Unsupported rich text schema version.");
    }
    if (document == null) {
      throw new IllegalArgumentException("Rich text document is required.");
    }
    document = document.deepCopy();
  }

  @Override
  public JsonNode document() {
    return document.deepCopy();
  }

  public static RichTextDocumentPayload current(JsonNode document) {
    return new RichTextDocumentPayload(RichTextSchema.CURRENT_VERSION, document);
  }
}

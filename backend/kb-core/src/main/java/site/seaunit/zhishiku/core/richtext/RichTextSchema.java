package site.seaunit.zhishiku.core.richtext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.util.Set;

public final class RichTextSchema {
  public static final int CURRENT_VERSION = 1;
  public static final String CONTRACT_ID =
      "https://seaunit.site/kb/contracts/rich-text.schema.json";

  private RichTextSchema() {}

  public static void validate(ObjectMapper mapper, String schemaJson, JsonNode document) {
    final JsonNode schemaNode;
    try {
      schemaNode = mapper.readTree(schemaJson);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Rich text JSON Schema could not be parsed.", error);
    }
    JsonSchema schema =
        JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaNode);
    Set<ValidationMessage> errors = schema.validate(document);
    if (!errors.isEmpty()) {
      String first = errors.iterator().next().getMessage();
      throw new RichTextException(
          "SCHEMA_INVALID", "Rich text JSON Schema validation failed: " + first);
    }
  }
}

package site.seaunit.zhishiku.core.richtext;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RichTextSchemaContractTest {
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void schemaAndDomainValidatorAgreeOnSupportedFixture() throws Exception {
    JsonNode document = supportedDocument();

    assertThatCode(() -> RichTextSchema.validate(mapper, Files.readString(schemaPath()), document))
        .doesNotThrowAnyException();
    assertThatCode(() -> RichTextValidator.defaults().validate(document))
        .doesNotThrowAnyException();
  }

  @Test
  void schemaAndDomainValidatorRejectDangerousOrUnknownContent() throws Exception {
    JsonNode document =
        mapper.readTree(
            """
            {"type":"doc","content":[{"type":"image","attrs":{
              "assetId":"data:image/svg+xml;base64,AAAA",
              "nodeId":"node-1","alt":"x","displayWidthPx":320,
              "aspectRatio":1,"alignment":"left","decorative":false,
              "src":"https://tracker.example/pixel"
            }}]}
            """);

    assertThatThrownBy(
            () -> RichTextSchema.validate(mapper, Files.readString(schemaPath()), document))
        .isInstanceOf(RichTextException.class)
        .extracting(error -> ((RichTextException) error).getCode())
        .isEqualTo("SCHEMA_INVALID");
    assertThatThrownBy(() -> RichTextValidator.defaults().validate(document))
        .isInstanceOf(RichTextException.class);
  }

  @Test
  void exposesTheSharedSchemaVersion() {
    org.assertj.core.api.Assertions.assertThat(RichTextSchema.CURRENT_VERSION).isEqualTo(1);
  }

  private JsonNode supportedDocument() throws Exception {
    return mapper.readTree(
        """
        {"type":"doc","content":[
          {"type":"paragraph","content":[{"type":"text","text":"正文"}]}
        ]}
        """);
  }

  private static Path schemaPath() {
    return Path.of(
        "..", "..", "specs", "004-rich-text-editor", "contracts", "rich-text.schema.json");
  }
}

package site.seaunit.zhishiku.richtext.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import site.seaunit.zhishiku.core.richtext.RichTextDocumentPayload;
import site.seaunit.zhishiku.core.richtext.RichTextException;
import site.seaunit.zhishiku.core.richtext.RichTextSchema;
import site.seaunit.zhishiku.core.richtext.RichTextValidator;

class RichTextSchemaContractTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final RichTextValidator validator = RichTextValidator.defaults();

  @Test
  void validRichTextFixturePassesSchemaAndDomainValidation() throws Exception {
    JsonNode document = validDocument();

    assertThatCode(() -> validateContract(document)).doesNotThrowAnyException();
    assertThatCode(() -> validator.validate(document)).doesNotThrowAnyException();
    assertThat(RichTextDocumentPayload.current(document).schemaVersion())
        .isEqualTo(RichTextSchema.CURRENT_VERSION);
  }

  @Test
  void unknownNodesAndDangerousAttributesFailBothValidationLayers() throws Exception {
    JsonNode unknown = mapper.readTree("{\"type\":\"doc\",\"content\":[{\"type\":\"script\"}]}");
    assertContractAndDomainReject(unknown);

    JsonNode dangerous =
        mapper.readTree(
            """
            {"type":"doc","content":[{"type":"paragraph","content":[
              {"type":"text","text":"x","marks":[{
                "type":"link","attrs":{"href":"javascript:alert(1)"}
              }]}
            ]}]}
            """);
    assertContractAndDomainReject(dangerous);
  }

  @Test
  void tableFixturePreservesHeaderAndBodySemantics() throws Exception {
    JsonNode table =
        mapper.readTree(
            """
            {"type":"doc","content":[{"type":"table","attrs":{"headerRow":true},"content":[
              {"type":"tableHeaderRow","content":[
                {"type":"tableHeader","content":[
                  {"type":"paragraph","content":[{"type":"text","text":"列"}]}
                ]}
              ]},
              {"type":"tableBodyRow","content":[
                {"type":"tableCell","content":[
                  {"type":"paragraph","content":[{"type":"text","text":"值"}]}
                ]}
              ]}
            ]}]}
            """);

    assertThatCode(() -> validateContract(table)).doesNotThrowAnyException();
    assertThatCode(() -> validator.validate(table)).doesNotThrowAnyException();
  }

  private void assertContractAndDomainReject(JsonNode document) {
    assertThatThrownBy(() -> validateContract(document)).isInstanceOf(RichTextException.class);
    assertThatThrownBy(() -> validator.validate(document)).isInstanceOf(RichTextException.class);
  }

  private void validateContract(JsonNode document) throws Exception {
    RichTextSchema.validate(mapper, Files.readString(schemaPath()), document);
  }

  private JsonNode validDocument() throws Exception {
    return mapper.readTree(
        """
        {"type":"doc","content":[
          {"type":"heading","attrs":{"level":2},"content":[
            {"type":"text","text":"标题"}
          ]},
          {"type":"paragraph","content":[
            {"type":"text","text":"正文","marks":[{"type":"bold"}]}
          ]},
          {"type":"bulletList","content":[
            {"type":"listItem","content":[
              {"type":"paragraph","content":[{"type":"text","text":"列表"}]}
            ]}
          ]}
        ]}
        """);
  }

  private static Path schemaPath() {
    return Path.of(
        "..", "..", "specs", "004-rich-text-editor", "contracts", "rich-text.schema.json");
  }
}

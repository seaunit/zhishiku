package site.seaunit.zhishiku.core.richtext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RichTextImageValidationTest {
  private static final UUID ASSET_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  private final ObjectMapper mapper = new ObjectMapper();
  private final RichTextValidator validator = RichTextValidator.defaults();

  @Test
  void returnsStableUniqueAssetIdsForRepeatedImageReferences() {
    ObjectNode document = document(image("node-1", ASSET_ID.toString()));
    document.withArray("content").add(image("node-2", ASSET_ID.toString()));

    RichTextValidation validation = validator.validate(document);

    assertThat(validation.imageCount()).isEqualTo(2);
    assertThat(validation.uniqueAssetIds()).containsExactly(ASSET_ID);
    assertThat(validation.images())
        .extracting(RichTextImageAsset::nodeId)
        .containsExactly("node-1", "node-2");
  }

  @Test
  void rejectsPseudoProtocolsAndOtherInvalidAssetReferences() {
    for (String value :
        new String[] {
          "data:image/png;base64,AAAA",
          "blob:https://example.com/id",
          "cid:inline-image",
          "file:///tmp/image.png",
          "https://tracker.example/image.png",
          "not-a-uuid"
        }) {
      ObjectNode document = document(image("node-1", value));

      assertThatThrownBy(() -> validator.validate(document))
          .isInstanceOf(RichTextException.class)
          .extracting(error -> ((RichTextException) error).getCode())
          .isEqualTo("IMAGE_ASSET_ID");
    }
  }

  @Test
  void rejectsDuplicateNodeIdsAndUnknownImageAttributes() {
    ObjectNode duplicate = document(image("same-node", ASSET_ID.toString()));
    duplicate.withArray("content").add(image("same-node", ASSET_ID.toString()));

    assertThatThrownBy(() -> validator.validate(duplicate))
        .isInstanceOf(RichTextException.class)
        .extracting(error -> ((RichTextException) error).getCode())
        .isEqualTo("IMAGE_NODE_ID_DUPLICATE");

    ObjectNode unsafe = document(image("node-1", ASSET_ID.toString()));
    ((ObjectNode) unsafe.withArray("content").get(0).path("attrs"))
        .put("src", "https://tracker.example/image.png");

    assertThatThrownBy(() -> validator.validate(unsafe))
        .isInstanceOf(RichTextException.class)
        .extracting(error -> ((RichTextException) error).getCode())
        .isEqualTo("UNKNOWN_ATTRIBUTE");
  }

  @Test
  void enforcesTwentyImageLimit() {
    ObjectNode document = document(image("node-0", ASSET_ID.toString()));
    ArrayNode content = document.withArray("content");
    for (int index = 1; index < 21; index++) {
      content.add(image("node-" + index, ASSET_ID.toString()));
    }

    assertThatThrownBy(() -> validator.validate(document))
        .isInstanceOf(RichTextException.class)
        .extracting(error -> ((RichTextException) error).getCode())
        .isEqualTo("TOO_MANY_IMAGES");
  }

  @Test
  void enforcesUniqueProcessedImageByteBudget() {
    RichTextValidation validation =
        validator.validate(
            document(image("node-1", ASSET_ID.toString()), image("node-2", ASSET_ID.toString())));

    assertThat(RichTextImageBudget.validate(validation, Map.of(ASSET_ID, 52_428_800L)))
        .isEqualTo(52_428_800L);

    assertThatThrownBy(
            () -> RichTextImageBudget.validate(validation, Map.of(ASSET_ID, 52_428_800L + 1)))
        .isInstanceOf(RichTextException.class)
        .extracting(error -> ((RichTextException) error).getCode())
        .isEqualTo("IMAGE_TOTAL_BYTES");
  }

  @Test
  void rejectsMissingOrNonPositiveImageMetadata() {
    RichTextValidation validation =
        validator.validate(document(image("node-1", ASSET_ID.toString())));

    assertThatThrownBy(() -> RichTextImageBudget.validate(validation, Map.of()))
        .isInstanceOf(RichTextException.class)
        .extracting(error -> ((RichTextException) error).getCode())
        .isEqualTo("IMAGE_METADATA_MISSING");

    assertThatThrownBy(() -> RichTextImageBudget.validate(validation, Map.of(ASSET_ID, 0L)))
        .isInstanceOf(RichTextException.class)
        .extracting(error -> ((RichTextException) error).getCode())
        .isEqualTo("IMAGE_METADATA_INVALID");
  }

  @Test
  void rejectsParentChildShapesOutsideTheSharedSchema() throws Exception {
    JsonNode listWithParagraph =
        mapper.readTree(
            """
            {"type":"doc","content":[{"type":"bulletList","content":[
              {"type":"paragraph","content":[{"type":"text","text":"not an item"}]}
            ]}]}
            """);
    assertThatThrownBy(() -> validator.validate(listWithParagraph))
        .isInstanceOf(RichTextException.class)
        .extracting(error -> ((RichTextException) error).getCode())
        .isEqualTo("LIST_CHILD");

    JsonNode quoteWithTable =
        mapper.readTree(
            """
            {"type":"doc","content":[{"type":"blockquote","content":[
              {"type":"table","attrs":{"headerRow":true},"content":[
                {"type":"tableHeaderRow","content":[
                  {"type":"tableHeader","content":[]}
                ]}
              ]}
            ]}]}
            """);
    assertThatThrownBy(() -> validator.validate(quoteWithTable))
        .isInstanceOf(RichTextException.class)
        .extracting(error -> ((RichTextException) error).getCode())
        .isEqualTo("BLOCKQUOTE_CHILD");
  }

  private ObjectNode document(ObjectNode... images) {
    ObjectNode document = mapper.createObjectNode();
    document.put("type", "doc");
    ArrayNode content = document.putArray("content");
    for (ObjectNode image : images) {
      content.add(image);
    }
    return document;
  }

  private ObjectNode image(String nodeId, String assetId) {
    ObjectNode image = mapper.createObjectNode();
    image.put("type", "image");
    ObjectNode attrs = image.putObject("attrs");
    attrs.put("assetId", assetId);
    attrs.put("nodeId", nodeId);
    attrs.put("alt", "示意图");
    attrs.put("caption", "图一");
    attrs.put("displayWidthPx", 720);
    attrs.put("aspectRatio", 1.5);
    attrs.put("alignment", "center");
    attrs.put("decorative", false);
    return image;
  }
}

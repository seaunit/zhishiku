package site.seaunit.zhishiku.ai.richtext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import site.seaunit.zhishiku.core.richtext.PlainTextProjector;
import site.seaunit.zhishiku.core.richtext.RichTextValidator;

class RichTextAiInputAssemblerTest {
  private static final UUID OWNER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID IMAGE_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID IMAGE_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

  @Test
  void sendsProjectedTextWithoutJsonOrInternalAttributes() throws Exception {
    FakeAssetRepository assets = new FakeAssetRepository();
    assets.add(IMAGE_A, "hash-a");
    RichTextAiInputAssembler assembler = assembler(assets);

    AiRichTextInput input = assembler.assemble(OWNER, document("正文", IMAGE_A), false);

    assertThat(input.plainText()).contains("正文", "图片说明", "替代文本");
    assertThat(input.plainText()).doesNotContain("\"type\"", "assetId", "nodeId", "storageKey");
    assertThat(input.images()).isEmpty();
    assertThat(assets.lookups).isZero();
  }

  @Test
  void deduplicatesRepeatedAssetsAndEqualContentHashesWhenExplicitlyAuthorized() throws Exception {
    FakeAssetRepository assets = new FakeAssetRepository();
    assets.add(IMAGE_A, "same-hash");
    assets.add(IMAGE_B, "same-hash");
    RichTextAiInputAssembler assembler = assembler(assets);

    AiRichTextInput input =
        assembler.assemble(OWNER, document("正文", IMAGE_A, IMAGE_A, IMAGE_B), true);

    assertThat(input.images()).hasSize(1);
    assertThat(input.images().get(0).contentHash()).isEqualTo("same-hash");
  }

  @Test
  void rejectsMissingOrUnsafeImagesWhenImageAnalysisWasAuthorized() throws Exception {
    RichTextAiInputAssembler assembler = assembler(new FakeAssetRepository());

    assertThatThrownBy(() -> assembler.assemble(OWNER, document("正文", IMAGE_A), true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not available");
  }

  private static RichTextAiInputAssembler assembler(FakeAssetRepository assets) {
    return new RichTextAiInputAssembler(
        RichTextValidator.defaults(), new PlainTextProjector(), assets);
  }

  private static JsonNode document(String text, UUID... assetIds) throws Exception {
    StringBuilder content = new StringBuilder();
    content.append(
        """
        {"type":"paragraph","content":[{"type":"text","text":"%s"}]}
        """
            .formatted(text));
    for (int index = 0; index < assetIds.length; index++) {
      content.append(
          """
          ,{"type":"image","attrs":{
            "assetId":"%s","nodeId":"img-%d","storageKey":"private/object/key",
            "alt":"替代文本","caption":"图片说明","displayWidthPx":720,
            "aspectRatio":1.5,"alignment":"center","decorative":false
          }}
          """
              .formatted(assetIds[index], index));
    }
    return new ObjectMapper().readTree("{\"type\":\"doc\",\"content\":[" + content + "]}");
  }

  private static final class FakeAssetRepository implements AiImageAssetRepository {
    private final Map<UUID, AiImageAsset> assets = new LinkedHashMap<>();
    private int lookups;

    private void add(UUID id, String hash) {
      assets.put(id, new AiImageAsset(id, hash));
    }

    @Override
    public Optional<AiImageAsset> findOwnedSafeAsset(UUID ownerId, UUID assetId) {
      lookups++;
      return OWNER.equals(ownerId) ? Optional.ofNullable(assets.get(assetId)) : Optional.empty();
    }
  }
}

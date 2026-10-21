package site.seaunit.zhishiku.ai.richtext;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.seaunit.zhishiku.core.richtext.PlainTextProjector;
import site.seaunit.zhishiku.core.richtext.RichTextValidator;

@Service
public class RichTextAiInputAssembler {
  private final PlainTextProjector projector;
  private final AiImageAssetRepository assets;

  @Autowired
  public RichTextAiInputAssembler(AiImageAssetRepository assets) {
    this(RichTextValidator.defaults(), new PlainTextProjector(), assets);
  }

  public RichTextAiInputAssembler(
      RichTextValidator validator, PlainTextProjector projector, AiImageAssetRepository assets) {
    this.projector = projector;
    this.assets = assets;
  }

  public AiRichTextInput assemble(
      UUID ownerId, JsonNode document, boolean imageAnalysisAuthorized) {
    if (document == null || !"doc".equals(document.path("type").asText())) {
      throw new IllegalArgumentException("A rich text document is required.");
    }
    String plainText = projector.project(document);
    if (!imageAnalysisAuthorized) {
      return new AiRichTextInput(plainText, List.of());
    }

    Set<UUID> requestedIds = new LinkedHashSet<>();
    collectImageIds(document, requestedIds);
    Map<String, AiImageAsset> uniqueByHash = new LinkedHashMap<>();
    for (UUID assetId : requestedIds) {
      AiImageAsset asset =
          assets
              .findOwnedSafeAsset(ownerId, assetId)
              .orElseThrow(
                  () -> new IllegalArgumentException("Authorized AI image is not available."));
      uniqueByHash.putIfAbsent(asset.contentHash(), asset);
    }
    return new AiRichTextInput(plainText, new ArrayList<>(uniqueByHash.values()));
  }

  private static void collectImageIds(JsonNode node, Set<UUID> ids) {
    if (node == null) {
      return;
    }
    if (node.isObject() && "image".equals(node.path("type").asText())) {
      ids.add(UUID.fromString(node.path("attrs").path("assetId").asText()));
    }
    if (node.isContainerNode()) {
      node.forEach(child -> collectImageIds(child, ids));
    }
  }
}

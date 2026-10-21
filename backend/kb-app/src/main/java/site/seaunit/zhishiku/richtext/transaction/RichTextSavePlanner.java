package site.seaunit.zhishiku.richtext.transaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import site.seaunit.zhishiku.core.richtext.PlainTextProjector;
import site.seaunit.zhishiku.core.richtext.RichTextImageBudget;
import site.seaunit.zhishiku.core.richtext.RichTextSchema;
import site.seaunit.zhishiku.core.richtext.RichTextValidation;
import site.seaunit.zhishiku.core.richtext.RichTextValidator;

@Component
public class RichTextSavePlanner {
  private final RichTextValidator validator;
  private final PlainTextProjector projector;
  private final ObjectMapper mapper;

  @Autowired
  public RichTextSavePlanner(ObjectMapper mapper) {
    this(RichTextValidator.defaults(), new PlainTextProjector(), mapper);
  }

  public RichTextSavePlanner(
      RichTextValidator validator, PlainTextProjector projector, ObjectMapper mapper) {
    this.validator = validator;
    this.projector = projector;
    this.mapper = mapper;
  }

  public RichTextSavePlan prepare(JsonNode document, Map<UUID, Long> processedSizeByAssetId) {
    RichTextValidation validation = validator.validate(document);
    long totalBytes = RichTextImageBudget.validate(validation, processedSizeByAssetId);
    JsonNode canonical = canonicalize(document);
    String json = writeJson(canonical);
    return new RichTextSavePlan(
        RichTextSchema.CURRENT_VERSION,
        canonical,
        projector.project(canonical),
        sha256(json),
        validation.imageCount(),
        totalBytes,
        validation.images());
  }

  private JsonNode canonicalize(JsonNode node) {
    if (node.isObject()) {
      ObjectNode sorted = mapper.createObjectNode();
      List<String> names = new ArrayList<>();
      Iterator<String> iterator = node.fieldNames();
      iterator.forEachRemaining(names::add);
      names.sort(String::compareTo);
      for (String name : names) {
        sorted.set(name, canonicalize(node.get(name)));
      }
      return sorted;
    }
    if (node.isArray()) {
      ArrayNode values = mapper.createArrayNode();
      node.forEach(value -> values.add(canonicalize(value)));
      return values;
    }
    return node.deepCopy();
  }

  private String writeJson(JsonNode document) {
    try {
      return mapper.writeValueAsString(document);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Rich text document could not be serialized.", error);
    }
  }

  private static String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 is unavailable.", error);
    }
  }
}

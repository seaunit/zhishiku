package site.seaunit.zhishiku.webimport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import site.seaunit.zhishiku.knowledge.KnowledgeDtos.KnowledgeItemResponse;

@Component
public class ImportedKnowledgeMapper {
  private final ObjectMapper mapper;

  public ImportedKnowledgeMapper(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public ObjectNode document(String body) {
    ObjectNode document = mapper.createObjectNode();
    document.put("type", "doc");
    ArrayNode content = document.putArray("content");
    String normalized = body.replace("\r\n", "\n").strip();
    for (String paragraphText : normalized.split("\\n{2,}")) {
      String paragraph = paragraphText.strip();
      if (paragraph.isBlank()) {
        continue;
      }
      ObjectNode node = content.addObject();
      node.put("type", "paragraph");
      ArrayNode inline = node.putArray("content");
      inline.addObject().put("type", "text").put("text", paragraph);
    }
    if (content.isEmpty()) {
      content.addObject().put("type", "paragraph");
    }
    return document;
  }

  public String note(
      OrganizedKnowledge note, boolean redactionApplied, String userNote, String sourceFormat) {
    List<String> parts = new ArrayList<>();
    if (sourceFormat != null && !sourceFormat.isBlank()) {
      parts.add("来源格式：" + sourceFormat.strip());
    }
    if (userNote != null && !userNote.isBlank()) {
      parts.add("用户备注：" + userNote.strip());
    }
    if (!note.summary().isBlank()) {
      parts.add("AI 摘要：" + note.summary());
    }
    if (!note.category().isBlank()) {
      parts.add("AI 分类：" + note.category());
    }
    if (!note.tags().isEmpty()) {
      parts.add("AI 标签：" + String.join("、", note.tags()));
    }
    if (redactionApplied) {
      parts.add("安全处理：已移除疑似密钥");
    }
    String noteText = String.join("\n", parts);
    return noteText.substring(0, Math.min(noteText.length(), 20_000));
  }

  public KnowledgeItemResponse withRedactionApplied(
      KnowledgeItemResponse item, boolean redactionApplied) {
    if (!redactionApplied || item.redactionApplied()) {
      return item;
    }
    return new KnowledgeItemResponse(
        item.id(),
        item.title(),
        item.body(),
        item.bodyDocument(),
        item.bodySchemaVersion(),
        item.bodyMigrationState(),
        item.note(),
        item.summary(),
        item.processingStatus(),
        item.lifecycleState(),
        true,
        item.source(),
        item.category(),
        item.tags(),
        item.version(),
        item.createdAt(),
        item.updatedAt());
  }
}

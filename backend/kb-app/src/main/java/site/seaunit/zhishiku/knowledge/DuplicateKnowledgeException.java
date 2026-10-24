package site.seaunit.zhishiku.knowledge;

import java.util.List;

public class DuplicateKnowledgeException extends RuntimeException {
  private final List<DuplicateKnowledge> existingItems;

  public DuplicateKnowledgeException(List<DuplicateKnowledge> existingItems) {
    super("This source is already in the knowledge library.");
    this.existingItems = List.copyOf(existingItems);
  }

  public List<DuplicateKnowledge> existingItems() {
    return existingItems;
  }
}

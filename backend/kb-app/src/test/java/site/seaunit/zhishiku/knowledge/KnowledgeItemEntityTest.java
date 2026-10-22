package site.seaunit.zhishiku.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class KnowledgeItemEntityTest {
  @Test
  void distinguishesOrdinaryAndOrganizedCreationStates() {
    assertThat(
            KnowledgeItemEntity.create(UUID.randomUUID(), "手动知识", "正文", "").getProcessingStatus())
        .isEqualTo("not_requested");
    assertThat(
            KnowledgeItemEntity.create(UUID.randomUUID(), "网页知识", "正文", "", "succeeded")
                .getProcessingStatus())
        .isEqualTo("succeeded");
  }
}

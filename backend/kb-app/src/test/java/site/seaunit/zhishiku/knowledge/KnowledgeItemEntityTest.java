package site.seaunit.zhishiku.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class KnowledgeItemEntityTest {
  @Test
  void distinguishesOrdinaryAndOrganizedCreationStates() {
    assertThat(
            KnowledgeItemEntity.create(UUID.randomUUID(), "手动知识", "正文", "").getProcessingStatus())
        .isEqualTo("not_requested");
    assertThat(
            KnowledgeItemEntity.create(UUID.randomUUID(), "网页知识", "正文", "", "review")
                .getProcessingStatus())
        .isEqualTo("review");
  }

  @Test
  void requiresExplicitReviewBeforeAnAiItemBecomesOrganized() {
    KnowledgeItemEntity item =
        KnowledgeItemEntity.create(UUID.randomUUID(), "网页知识", "正文", "", "review");

    item.markReviewed();
    assertThat(item.getProcessingStatus()).isEqualTo("succeeded");

    item.markReviewed();
    assertThat(item.getProcessingStatus()).isEqualTo("succeeded");
  }

  @Test
  void returnsGeneratedItemsToReviewAfterReanalysis() {
    KnowledgeItemEntity item =
        KnowledgeItemEntity.create(UUID.randomUUID(), "网页知识", "正文", "", "succeeded");

    item.markForReview();

    assertThat(item.getProcessingStatus()).isEqualTo("review");
  }

  @Test
  void rejectsReviewingManualItems() {
    KnowledgeItemEntity item = KnowledgeItemEntity.create(UUID.randomUUID(), "手动知识", "正文", "");

    assertThatThrownBy(item::markReviewed)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("awaiting review");
  }
}

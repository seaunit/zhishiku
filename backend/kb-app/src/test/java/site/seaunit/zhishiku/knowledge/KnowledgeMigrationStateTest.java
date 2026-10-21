package site.seaunit.zhishiku.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KnowledgeMigrationStateTest {
  @Test
  void returnsPendingWhenLegacyTextHasNoRichDocument() {
    assertThat(KnowledgeService.effectiveMigrationState("completed", false)).isEqualTo("pending");
    assertThat(KnowledgeService.effectiveMigrationState(null, false)).isEqualTo("pending");
  }

  @Test
  void preservesFailureAndCompletedStatesWithTheCorrectDocumentPresence() {
    assertThat(KnowledgeService.effectiveMigrationState("failed", false)).isEqualTo("failed");
    assertThat(KnowledgeService.effectiveMigrationState("completed", true)).isEqualTo("completed");
    assertThat(KnowledgeService.effectiveMigrationState("not_required", true))
        .isEqualTo("not_required");
  }
}

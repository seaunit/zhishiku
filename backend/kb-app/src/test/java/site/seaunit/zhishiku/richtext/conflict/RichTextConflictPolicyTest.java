package site.seaunit.zhishiku.richtext.conflict;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import site.seaunit.zhishiku.knowledge.RichTextConflictException;

class RichTextConflictPolicyTest {
  private final RichTextConflictPolicy policy = new RichTextConflictPolicy();

  @Test
  void rejectsChangedVersionByDefault() {
    assertThat(policy.decide(2, 3, RichTextConflictPolicy.ConflictResolution.REJECT))
        .isEqualTo(RichTextConflictPolicy.ConflictDecision.REJECT);
  }

  @Test
  void allowsOnlyExplicitOverwriteOrSaveAsNew() {
    assertThat(policy.decide(2, 3, RichTextConflictPolicy.ConflictResolution.OVERWRITE))
        .isEqualTo(RichTextConflictPolicy.ConflictDecision.APPLY);
    assertThat(policy.decide(2, 3, RichTextConflictPolicy.ConflictResolution.SAVE_AS_NEW))
        .isEqualTo(RichTextConflictPolicy.ConflictDecision.APPLY_AS_NEW);
    assertThat(policy.decide(3, 3, RichTextConflictPolicy.ConflictResolution.SAVE_AS_NEW))
        .isEqualTo(RichTextConflictPolicy.ConflictDecision.APPLY);
  }

  @Test
  void parsingUnknownResolutionFallsBackToReject() {
    assertThat(RichTextConflictPolicy.ConflictResolution.parse(null))
        .isEqualTo(RichTextConflictPolicy.ConflictResolution.REJECT);
    assertThat(RichTextConflictPolicy.ConflictResolution.parse("unknown"))
        .isEqualTo(RichTextConflictPolicy.ConflictResolution.REJECT);
  }

  @Test
  void exposesCurrentVersionWhenRejecting() {
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> policy.requireCanWrite(1, 9, "reject"))
        .isInstanceOf(RichTextConflictException.class)
        .extracting("currentVersion")
        .isEqualTo(9L);
  }
}

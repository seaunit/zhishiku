package site.seaunit.zhishiku.richtext.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RichTextLifecyclePolicyTest {
  private final RichTextLifecyclePolicy policy = new RichTextLifecyclePolicy();

  @Test
  void permitsRecycleRestoreAndPurgeInOrder() {
    assertThat(policy.next("active", "recycle")).isEqualTo("recycled");
    assertThat(policy.next("recycled", "restore")).isEqualTo("active");
    assertThat(policy.next("recycled", "purge")).isEqualTo("purging");
  }

  @Test
  void rejectsInvalidLifecycleTransitions() {
    assertThatThrownBy(() -> policy.next("active", "restore"))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> policy.next("active", "purge"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void deletesOnlyUnreferencedAttachmentsAfterItemPurge() {
    assertThat(policy.attachmentEligibleForCleanup(0, 0, true)).isTrue();
    assertThat(policy.attachmentEligibleForCleanup(1, 0, true)).isFalse();
    assertThat(policy.attachmentEligibleForCleanup(0, 1, true)).isFalse();
    assertThat(policy.attachmentEligibleForCleanup(0, 0, false)).isFalse();
  }
}

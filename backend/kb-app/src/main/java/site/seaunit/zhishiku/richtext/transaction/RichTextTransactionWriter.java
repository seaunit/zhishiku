package site.seaunit.zhishiku.richtext.transaction;

import java.util.UUID;

public interface RichTextTransactionWriter {
  long currentVersion(UUID itemId, UUID ownerId);

  void commit(RichTextCommitCommand command);
}

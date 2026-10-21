package site.seaunit.zhishiku.richtext.image;

import java.util.UUID;

public interface RichTextItemAccess {
  void requireWritableItem(UUID itemId, UUID ownerId);
}

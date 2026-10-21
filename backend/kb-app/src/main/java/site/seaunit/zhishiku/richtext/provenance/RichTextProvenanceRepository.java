package site.seaunit.zhishiku.richtext.provenance;

import java.util.UUID;

public interface RichTextProvenanceRepository {
  boolean ownsItem(UUID itemId, UUID ownerId);

  void insertSource(UUID itemId, NormalizedRichTextSource source);

  void insertConversionReport(NormalizedRichTextSource source);
}

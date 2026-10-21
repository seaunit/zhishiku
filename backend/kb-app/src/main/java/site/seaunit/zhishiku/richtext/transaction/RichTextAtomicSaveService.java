package site.seaunit.zhishiku.richtext.transaction;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.seaunit.zhishiku.richtext.provenance.NormalizedRichTextSource;
import site.seaunit.zhishiku.richtext.provenance.RichTextConversionReport;
import site.seaunit.zhishiku.richtext.provenance.RichTextSourceCommand;
import site.seaunit.zhishiku.richtext.provenance.RichTextSourcePolicy;

@Service
public class RichTextAtomicSaveService {
  private final RichTextTransactionWriter writer;
  private final RichTextSourcePolicy sourcePolicy;

  public RichTextAtomicSaveService(
      RichTextTransactionWriter writer, RichTextSourcePolicy sourcePolicy) {
    this.writer = writer;
    this.sourcePolicy = sourcePolicy;
  }

  @Transactional
  public void save(
      UUID ownerId,
      UUID itemId,
      long expectedVersion,
      RichTextSavePlan plan,
      RichTextSourceCommand sourceCommand,
      RichTextConversionReport report) {
    long currentVersion = writer.currentVersion(itemId, ownerId);
    if (currentVersion != expectedVersion) {
      throw new RichTextVersionConflictException(currentVersion);
    }
    NormalizedRichTextSource source =
        sourcePolicy.normalize(sourceCommand, report).withSourceId(UUID.randomUUID());
    writer.commit(new RichTextCommitCommand(ownerId, itemId, expectedVersion, plan, source));
  }
}

package site.seaunit.zhishiku.richtext.provenance;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RichTextProvenanceService {
  private final RichTextProvenanceRepository repository;
  private final RichTextSourcePolicy policy;

  public RichTextProvenanceService(
      RichTextProvenanceRepository repository, RichTextSourcePolicy policy) {
    this.repository = repository;
    this.policy = policy;
  }

  @Transactional
  public NormalizedRichTextSource record(
      UUID ownerId, UUID itemId, RichTextSourceCommand command, RichTextConversionReport report) {
    if (!repository.ownsItem(itemId, ownerId)) {
      throw new IllegalArgumentException("Knowledge item was not found for the current owner.");
    }
    NormalizedRichTextSource source =
        policy.normalize(command, report).withSourceId(UUID.randomUUID());
    repository.insertSource(itemId, source);
    repository.insertConversionReport(source);
    return source;
  }
}

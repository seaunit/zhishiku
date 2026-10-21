package site.seaunit.zhishiku.richtext.transaction;

import java.util.UUID;
import site.seaunit.zhishiku.richtext.provenance.NormalizedRichTextSource;

public record RichTextCommitCommand(
    UUID ownerId,
    UUID itemId,
    long expectedVersion,
    RichTextSavePlan plan,
    NormalizedRichTextSource source) {}

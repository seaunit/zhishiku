package site.seaunit.zhishiku.richtext.conflict;

import java.util.Locale;
import org.springframework.stereotype.Component;
import site.seaunit.zhishiku.knowledge.RichTextConflictException;

@Component
public class RichTextConflictPolicy {
  public enum ConflictResolution {
    REJECT,
    OVERWRITE,
    SAVE_AS_NEW;

    public static ConflictResolution parse(String value) {
      if (value == null || value.isBlank()) {
        return REJECT;
      }
      return switch (value.strip().toLowerCase(Locale.ROOT).replace('-', '_')) {
        case "overwrite" -> OVERWRITE;
        case "save_as_new" -> SAVE_AS_NEW;
        default -> REJECT;
      };
    }
  }

  public enum ConflictDecision {
    APPLY,
    APPLY_AS_NEW,
    REJECT
  }

  public ConflictDecision decide(
      long expectedVersion, long currentVersion, ConflictResolution resolution) {
    if (expectedVersion == currentVersion) {
      return ConflictDecision.APPLY;
    }
    return switch (resolution) {
      case OVERWRITE -> ConflictDecision.APPLY;
      case SAVE_AS_NEW -> ConflictDecision.APPLY_AS_NEW;
      case REJECT -> ConflictDecision.REJECT;
    };
  }

  public void requireCanWrite(long expectedVersion, long currentVersion, String resolution) {
    ConflictDecision decision =
        decide(expectedVersion, currentVersion, ConflictResolution.parse(resolution));
    if (decision == ConflictDecision.REJECT) {
      throw new RichTextConflictException(currentVersion);
    }
  }
}

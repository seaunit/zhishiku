package site.seaunit.zhishiku.richtext.lifecycle;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class RichTextLifecyclePolicy {
  public String next(String currentState, String action) {
    String current = normalize(currentState);
    String requested = normalize(action);
    if ("active".equals(current) && "recycle".equals(requested)) {
      return "recycled";
    }
    if ("recycled".equals(current) && "restore".equals(requested)) {
      return "active";
    }
    if ("recycled".equals(current) && "purge".equals(requested)) {
      return "purging";
    }
    throw new IllegalStateException(
        "Lifecycle transition is not allowed: " + currentState + " -> " + action);
  }

  public boolean attachmentEligibleForCleanup(
      int activeReferences, int retainedReferences, boolean itemPurged) {
    return itemPurged && activeReferences == 0 && retainedReferences == 0;
  }

  private static String normalize(String value) {
    return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
  }
}

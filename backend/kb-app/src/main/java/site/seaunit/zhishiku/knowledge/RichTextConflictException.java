package site.seaunit.zhishiku.knowledge;

public class RichTextConflictException extends RuntimeException {
  private final long currentVersion;

  public RichTextConflictException(long currentVersion) {
    super("The knowledge item version has changed.");
    this.currentVersion = currentVersion;
  }

  public long getCurrentVersion() {
    return currentVersion;
  }
}

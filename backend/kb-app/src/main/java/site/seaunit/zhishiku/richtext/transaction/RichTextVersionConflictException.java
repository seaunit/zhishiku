package site.seaunit.zhishiku.richtext.transaction;

public class RichTextVersionConflictException extends RuntimeException {
  private final long currentVersion;

  public RichTextVersionConflictException(long currentVersion) {
    super("Rich text version conflict; current version is " + currentVersion + ".");
    this.currentVersion = currentVersion;
  }

  public long getCurrentVersion() {
    return currentVersion;
  }
}

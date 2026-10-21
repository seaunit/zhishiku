package site.seaunit.zhishiku.core.richtext;

public class RichTextException extends RuntimeException {
  private final String code;

  public RichTextException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}

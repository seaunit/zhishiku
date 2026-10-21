package site.seaunit.zhishiku.richtext.image;

public class ImageInspectionException extends RuntimeException {
  private final String code;

  public ImageInspectionException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}

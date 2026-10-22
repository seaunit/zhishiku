package site.seaunit.zhishiku.textimport;

import org.springframework.http.HttpStatus;

public class TextImportException extends RuntimeException {
  private final String code;
  private final HttpStatus status;

  public TextImportException(String code, HttpStatus status, String message) {
    super(message);
    this.code = code;
    this.status = status;
  }

  public String getCode() {
    return code;
  }

  public HttpStatus getStatus() {
    return status;
  }
}

package site.seaunit.zhishiku.documentimport;

import org.springframework.http.HttpStatus;

public class DocumentImportException extends RuntimeException {
  private final String code;
  private final HttpStatus status;

  public DocumentImportException(String code, HttpStatus status, String message) {
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

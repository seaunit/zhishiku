package site.seaunit.zhishiku.auth.registration;

public class EmailDomainNotAllowedException extends RuntimeException {
  public EmailDomainNotAllowedException() {
    super("Disposable email domains are not allowed.");
  }
}

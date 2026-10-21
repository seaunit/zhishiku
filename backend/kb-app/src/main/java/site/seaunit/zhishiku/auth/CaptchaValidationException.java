package site.seaunit.zhishiku.auth;

public class CaptchaValidationException extends RuntimeException {
  public CaptchaValidationException() {
    super("Captcha is invalid or expired.");
  }
}

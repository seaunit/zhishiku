package site.seaunit.zhishiku.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import site.seaunit.zhishiku.auth.CaptchaValidationException;
import site.seaunit.zhishiku.auth.registration.EmailCodeInvalidException;
import site.seaunit.zhishiku.auth.registration.EmailDeliveryUnavailableException;
import site.seaunit.zhishiku.auth.registration.EmailDomainNotAllowedException;
import site.seaunit.zhishiku.auth.registration.InvalidEmailAddressException;
import site.seaunit.zhishiku.auth.registration.PasswordConfirmationMismatchException;
import site.seaunit.zhishiku.auth.registration.RateLimitedException;
import site.seaunit.zhishiku.auth.registration.RegistrationClosedException;
import site.seaunit.zhishiku.core.richtext.RichTextException;
import site.seaunit.zhishiku.knowledge.RichTextConflictException;
import site.seaunit.zhishiku.richtext.image.ImageInspectionException;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(RegistrationClosedException.class)
  ResponseEntity<ErrorResponse> registrationClosed(
      RegistrationClosedException error, HttpServletRequest request) {
    return response(HttpStatus.CONFLICT, "REGISTRATION_CLOSED", error.getMessage(), request);
  }

  @ExceptionHandler(InvalidEmailAddressException.class)
  ResponseEntity<ErrorResponse> invalidEmail(
      InvalidEmailAddressException error, HttpServletRequest request) {
    return response(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_EMAIL", error.getMessage(), request);
  }

  @ExceptionHandler(EmailDomainNotAllowedException.class)
  ResponseEntity<ErrorResponse> disposableEmail(
      EmailDomainNotAllowedException error, HttpServletRequest request) {
    return response(
        HttpStatus.UNPROCESSABLE_ENTITY, "EMAIL_DOMAIN_NOT_ALLOWED", error.getMessage(), request);
  }

  @ExceptionHandler(EmailCodeInvalidException.class)
  ResponseEntity<ErrorResponse> invalidEmailCode(
      EmailCodeInvalidException error, HttpServletRequest request) {
    return response(
        HttpStatus.UNPROCESSABLE_ENTITY, "EMAIL_CODE_INVALID", error.getMessage(), request);
  }

  @ExceptionHandler(PasswordConfirmationMismatchException.class)
  ResponseEntity<ErrorResponse> passwordMismatch(
      PasswordConfirmationMismatchException error, HttpServletRequest request) {
    return response(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "PASSWORD_CONFIRMATION_MISMATCH",
        error.getMessage(),
        request);
  }

  @ExceptionHandler(EmailDeliveryUnavailableException.class)
  ResponseEntity<ErrorResponse> emailUnavailable(
      EmailDeliveryUnavailableException error, HttpServletRequest request) {
    return response(
        HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_DELIVERY_UNAVAILABLE", error.getMessage(), request);
  }

  @ExceptionHandler(RateLimitedException.class)
  ResponseEntity<ErrorResponse> rateLimited(
      RateLimitedException error, HttpServletRequest request) {
    String requestId = String.valueOf(request.getAttribute(RequestIdFilter.ATTRIBUTE));
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header("Retry-After", String.valueOf(error.getRetryAfterSeconds()))
        .body(new ErrorResponse("RATE_LIMITED", error.getMessage(), requestId, true));
  }

  @ExceptionHandler(CaptchaValidationException.class)
  ResponseEntity<ErrorResponse> captcha(
      CaptchaValidationException error, HttpServletRequest request) {
    return response(
        HttpStatus.UNPROCESSABLE_ENTITY, "CAPTCHA_INVALID", error.getMessage(), request);
  }

  @ExceptionHandler(RichTextException.class)
  ResponseEntity<ErrorResponse> richText(RichTextException error, HttpServletRequest request) {
    return response(HttpStatus.UNPROCESSABLE_ENTITY, error.getCode(), error.getMessage(), request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ErrorResponse> invalid(
      IllegalArgumentException error, HttpServletRequest request) {
    return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", error.getMessage(), request);
  }

  @ExceptionHandler(ImageInspectionException.class)
  ResponseEntity<ErrorResponse> image(ImageInspectionException error, HttpServletRequest request) {
    return response(HttpStatus.UNPROCESSABLE_ENTITY, error.getCode(), error.getMessage(), request);
  }

  @ExceptionHandler(IllegalStateException.class)
  ResponseEntity<ErrorResponse> state(IllegalStateException error, HttpServletRequest request) {
    return response(HttpStatus.CONFLICT, "STATE_CONFLICT", error.getMessage(), request);
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ResponseEntity<ErrorResponse> conflict(
      OptimisticLockingFailureException error, HttpServletRequest request) {
    return response(
        HttpStatus.CONFLICT,
        "RICH_TEXT_VERSION_CONFLICT",
        "The knowledge item changed after this editor was loaded.",
        request);
  }

  @ExceptionHandler(RichTextConflictException.class)
  ResponseEntity<ErrorResponse> richTextConflict(
      RichTextConflictException error, HttpServletRequest request) {
    return response(
        HttpStatus.CONFLICT,
        "RICH_TEXT_VERSION_CONFLICT",
        "Current version is " + error.getCurrentVersion() + ".",
        request);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<ErrorResponse> tooLarge(
      MaxUploadSizeExceededException error, HttpServletRequest request) {
    return response(
        HttpStatus.PAYLOAD_TOO_LARGE, "UPLOAD_TOO_LARGE", "File exceeds 25 MiB.", request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErrorResponse> validation(
      MethodArgumentNotValidException error, HttpServletRequest request) {
    return response(
        HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED", "Request is invalid.", request);
  }

  private static ResponseEntity<ErrorResponse> response(
      HttpStatus status, String code, String message, HttpServletRequest request) {
    String requestId = String.valueOf(request.getAttribute(RequestIdFilter.ATTRIBUTE));
    return ResponseEntity.status(status)
        .body(new ErrorResponse(code, message, requestId, status.is5xxServerError()));
  }
}

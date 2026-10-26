package site.seaunit.zhishiku.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import site.seaunit.zhishiku.admin.AdminAccountException;
import site.seaunit.zhishiku.auth.CaptchaValidationException;
import site.seaunit.zhishiku.auth.registration.EmailAlreadyRegisteredException;
import site.seaunit.zhishiku.auth.registration.EmailCodeInvalidException;
import site.seaunit.zhishiku.auth.registration.EmailDeliveryUnavailableException;
import site.seaunit.zhishiku.auth.registration.EmailDomainNotAllowedException;
import site.seaunit.zhishiku.auth.registration.InvalidEmailAddressException;
import site.seaunit.zhishiku.auth.registration.PasswordConfirmationMismatchException;
import site.seaunit.zhishiku.auth.registration.RateLimitedException;
import site.seaunit.zhishiku.auth.registration.RegistrationClosedException;
import site.seaunit.zhishiku.billing.BillingException;
import site.seaunit.zhishiku.core.richtext.RichTextException;
import site.seaunit.zhishiku.documentimport.DocumentImportException;
import site.seaunit.zhishiku.idempotency.IdempotencyException;
import site.seaunit.zhishiku.knowledge.DuplicateKnowledgeException;
import site.seaunit.zhishiku.knowledge.RichTextConflictException;
import site.seaunit.zhishiku.richtext.image.ImageInspectionException;
import site.seaunit.zhishiku.screenshotimport.ScreenshotImportException;
import site.seaunit.zhishiku.search.external.ExternalSearchCandidateNotFoundException;
import site.seaunit.zhishiku.search.external.ExternalSearchUnavailableException;
import site.seaunit.zhishiku.search.external.ExternalSourceUnavailableException;
import site.seaunit.zhishiku.textimport.TextImportException;
import site.seaunit.zhishiku.webimport.WebImportBillingRequiredException;
import site.seaunit.zhishiku.webimport.WebImportConsentRequiredException;
import site.seaunit.zhishiku.webimport.WebImportUnavailableException;
import site.seaunit.zhishiku.webimport.WebPageNoTextException;
import site.seaunit.zhishiku.webimport.WebPageOrganizationException;
import site.seaunit.zhishiku.webimport.WebPageUrlException;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<ErrorResponse> authentication(
      AuthenticationException error, HttpServletRequest request) {
    return response(
        HttpStatus.UNAUTHORIZED,
        "AUTHENTICATION_FAILED",
        "The account or password is invalid, or the account is disabled.",
        request);
  }

  @ExceptionHandler(AdminAccountException.class)
  ResponseEntity<ErrorResponse> adminAccount(
      AdminAccountException error, HttpServletRequest request) {
    return response(error.getStatus(), error.getCode(), error.getMessage(), request);
  }

  @ExceptionHandler(IdempotencyException.class)
  ResponseEntity<ErrorResponse> idempotency(
      IdempotencyException error, HttpServletRequest request) {
    HttpStatus status =
        "REQUEST_IN_PROGRESS".equals(error.getCode())
                || "IDEMPOTENCY_CONFLICT".equals(error.getCode())
            ? HttpStatus.CONFLICT
            : HttpStatus.BAD_REQUEST;
    return response(status, error.getCode(), error.getMessage(), request);
  }

  @ExceptionHandler(BillingException.class)
  ResponseEntity<Map<String, Object>> billing(BillingException error, HttpServletRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("code", error.getCode());
    body.put("message", error.getMessage());
    body.put("requestId", String.valueOf(request.getAttribute(RequestIdFilter.ATTRIBUTE)));
    body.put("retryable", error.getStatus().is5xxServerError());
    body.putAll(error.getDetails());
    return ResponseEntity.status(error.getStatus()).body(body);
  }

  @ExceptionHandler(EmailAlreadyRegisteredException.class)
  ResponseEntity<ErrorResponse> emailAlreadyRegistered(
      EmailAlreadyRegisteredException error, HttpServletRequest request) {
    return response(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", error.getMessage(), request);
  }

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

  @ExceptionHandler(ExternalSearchUnavailableException.class)
  ResponseEntity<ErrorResponse> externalSearchUnavailable(
      ExternalSearchUnavailableException error, HttpServletRequest request) {
    return response(
        HttpStatus.SERVICE_UNAVAILABLE, "EXTERNAL_SEARCH_UNAVAILABLE", error.getMessage(), request);
  }

  @ExceptionHandler(ExternalSearchCandidateNotFoundException.class)
  ResponseEntity<ErrorResponse> externalCandidateNotFound(
      ExternalSearchCandidateNotFoundException error, HttpServletRequest request) {
    return response(
        HttpStatus.NOT_FOUND, "EXTERNAL_CANDIDATE_NOT_FOUND", error.getMessage(), request);
  }

  @ExceptionHandler(ExternalSourceUnavailableException.class)
  ResponseEntity<ErrorResponse> externalSourceUnavailable(
      ExternalSourceUnavailableException error, HttpServletRequest request) {
    return response(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "EXTERNAL_SOURCE_UNAVAILABLE",
        error.getMessage(),
        request);
  }

  @ExceptionHandler(DuplicateKnowledgeException.class)
  ResponseEntity<Map<String, Object>> duplicateKnowledge(
      DuplicateKnowledgeException error, HttpServletRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("code", "KNOWLEDGE_ALREADY_IMPORTED");
    body.put("message", error.getMessage());
    body.put("requestId", String.valueOf(request.getAttribute(RequestIdFilter.ATTRIBUTE)));
    body.put("retryable", false);
    body.put(
        "existingItems",
        error.existingItems().stream()
            .map(item -> Map.of("id", item.itemId(), "title", item.title()))
            .toList());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
  }

  @ExceptionHandler(WebImportUnavailableException.class)
  ResponseEntity<ErrorResponse> webImportUnavailable(
      WebImportUnavailableException error, HttpServletRequest request) {
    return response(
        HttpStatus.SERVICE_UNAVAILABLE, "WEB_IMPORT_UNAVAILABLE", error.getMessage(), request);
  }

  @ExceptionHandler(WebImportConsentRequiredException.class)
  ResponseEntity<ErrorResponse> webImportConsent(
      WebImportConsentRequiredException error, HttpServletRequest request) {
    return response(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "AI_EXTERNAL_CONSENT_REQUIRED",
        error.getMessage(),
        request);
  }

  @ExceptionHandler(WebImportBillingRequiredException.class)
  ResponseEntity<ErrorResponse> webImportBilling(
      WebImportBillingRequiredException error, HttpServletRequest request) {
    return response(
        HttpStatus.PAYMENT_REQUIRED, "AI_BILLING_REQUIRED", error.getMessage(), request);
  }

  @ExceptionHandler(DocumentImportException.class)
  ResponseEntity<ErrorResponse> documentImport(
      DocumentImportException error, HttpServletRequest request) {
    return response(error.getStatus(), error.getCode(), error.getMessage(), request);
  }

  @ExceptionHandler(ScreenshotImportException.class)
  ResponseEntity<ErrorResponse> screenshotImport(
      ScreenshotImportException error, HttpServletRequest request) {
    return response(error.getStatus(), error.getCode(), error.getMessage(), request);
  }

  @ExceptionHandler(TextImportException.class)
  ResponseEntity<ErrorResponse> textImport(TextImportException error, HttpServletRequest request) {
    return response(error.getStatus(), error.getCode(), error.getMessage(), request);
  }

  @ExceptionHandler(WebPageUrlException.class)
  ResponseEntity<ErrorResponse> webPageUrl(WebPageUrlException error, HttpServletRequest request) {
    return response(HttpStatus.BAD_REQUEST, "WEB_PAGE_INVALID_URL", error.getMessage(), request);
  }

  @ExceptionHandler(WebPageNoTextException.class)
  ResponseEntity<ErrorResponse> webPageNoText(
      WebPageNoTextException error, HttpServletRequest request) {
    return response(
        HttpStatus.UNPROCESSABLE_ENTITY, "WEB_PAGE_NO_TEXT", error.getMessage(), request);
  }

  @ExceptionHandler(WebPageOrganizationException.class)
  ResponseEntity<ErrorResponse> webPageOrganization(
      WebPageOrganizationException error, HttpServletRequest request) {
    return response(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "WEB_PAGE_ORGANIZATION_FAILED",
        error.getMessage(),
        request);
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

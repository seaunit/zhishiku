package site.seaunit.zhishiku.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import site.seaunit.zhishiku.core.richtext.RichTextException;
import site.seaunit.zhishiku.knowledge.RichTextConflictException;
import site.seaunit.zhishiku.richtext.image.ImageInspectionException;

@RestControllerAdvice
public class ApiExceptionHandler {
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

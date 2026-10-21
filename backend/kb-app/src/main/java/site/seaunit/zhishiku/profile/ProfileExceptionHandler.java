package site.seaunit.zhishiku.profile;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import site.seaunit.zhishiku.common.ErrorResponse;
import site.seaunit.zhishiku.common.RequestIdFilter;

@RestControllerAdvice(assignableTypes = ProfileController.class)
public class ProfileExceptionHandler {
  @ExceptionHandler(InvalidDisplayNameException.class)
  ResponseEntity<ErrorResponse> invalidDisplayName(
      InvalidDisplayNameException error, HttpServletRequest request) {
    String requestId = String.valueOf(request.getAttribute(RequestIdFilter.ATTRIBUTE));
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(
            new ErrorResponse(
                "DISPLAY_NAME_INVALID",
                "Display name must contain 1 to 40 characters without control characters or angle brackets.",
                requestId,
                false));
  }
}

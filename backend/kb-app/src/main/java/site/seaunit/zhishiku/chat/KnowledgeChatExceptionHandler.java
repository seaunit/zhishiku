package site.seaunit.zhishiku.chat;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import site.seaunit.zhishiku.common.ErrorResponse;
import site.seaunit.zhishiku.common.RequestIdFilter;

@RestControllerAdvice
public class KnowledgeChatExceptionHandler {
  @ExceptionHandler(KnowledgeChatConversationNotFoundException.class)
  ResponseEntity<ErrorResponse> notFound(
      KnowledgeChatConversationNotFoundException error, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            new ErrorResponse(
                "CHAT_CONVERSATION_NOT_FOUND",
                error.getMessage(),
                String.valueOf(request.getAttribute(RequestIdFilter.ATTRIBUTE)),
                false));
  }
}

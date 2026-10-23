package site.seaunit.zhishiku.chat;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import site.seaunit.zhishiku.billing.BillingException;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.AnswerRequest;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.AnswerResponse;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.AppendMessageRequest;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.ChatConversationDetail;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.ChatConversationPage;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.ChatConversationResponse;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.ChatMessageResponse;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.ConversationDeleteResponse;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.CreateConversationRequest;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.RetrievalRequest;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.RetrievalResponse;
import site.seaunit.zhishiku.search.external.ExternalSearchUnavailableException;
import site.seaunit.zhishiku.webimport.WebImportBillingRequiredException;
import site.seaunit.zhishiku.webimport.WebImportConsentRequiredException;
import site.seaunit.zhishiku.webimport.WebImportUnavailableException;

@RestController
@RequestMapping("/api/v1/chat")
public class KnowledgeChatController {
  private final KnowledgeChatService service;
  private final KnowledgeChatConversationService conversations;

  public KnowledgeChatController(
      KnowledgeChatService service, KnowledgeChatConversationService conversations) {
    this.service = service;
    this.conversations = conversations;
  }

  @GetMapping("/conversations")
  ChatConversationPage conversations(
      @RequestParam(name = "limit", defaultValue = "20") int limit, Authentication authentication) {
    return conversations.list(authentication, limit);
  }

  @PostMapping("/conversations")
  @ResponseStatus(HttpStatus.CREATED)
  ChatConversationResponse createConversation(
      @Valid @RequestBody CreateConversationRequest request,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      Authentication authentication) {
    return conversations.create(authentication, request, idempotencyKey);
  }

  @GetMapping("/conversations/{conversationId}")
  ChatConversationDetail conversation(
      @PathVariable UUID conversationId, Authentication authentication) {
    return conversations.detail(authentication, conversationId);
  }

  @PostMapping("/conversations/{conversationId}/messages")
  @ResponseStatus(HttpStatus.CREATED)
  ChatMessageResponse appendMessage(
      @PathVariable UUID conversationId,
      @Valid @RequestBody AppendMessageRequest request,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      Authentication authentication) {
    return conversations.append(authentication, conversationId, request, idempotencyKey);
  }

  @DeleteMapping("/conversations/{conversationId}")
  ConversationDeleteResponse deleteConversation(
      @PathVariable UUID conversationId,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      Authentication authentication) {
    return conversations.delete(authentication, conversationId, idempotencyKey);
  }

  @PostMapping("/retrieval")
  RetrievalResponse retrieve(
      @Valid @RequestBody RetrievalRequest request, Authentication authentication) {
    return service.retrieve(request.query(), authentication);
  }

  @PostMapping("/answers")
  AnswerResponse answer(
      @Valid @RequestBody AnswerRequest request,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      Authentication authentication) {
    return service.answer(request, idempotencyKey, authentication);
  }

  @PostMapping(value = "/answers/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  SseEmitter answerStream(
      @Valid @RequestBody AnswerRequest request,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      Authentication authentication) {
    return stream(
        request,
        idempotencyKey,
        authentication,
        (key, tokenConsumer) -> service.streamAnswer(request, key, authentication, tokenConsumer));
  }

  @PostMapping("/internet")
  AnswerResponse internet(
      @Valid @RequestBody AnswerRequest request,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      Authentication authentication) {
    return service.answerWithInternet(request, idempotencyKey, authentication);
  }

  @PostMapping(value = "/internet/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  SseEmitter internetStream(
      @Valid @RequestBody AnswerRequest request,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      Authentication authentication) {
    return stream(
        request,
        idempotencyKey,
        authentication,
        (key, tokenConsumer) ->
            service.streamInternetAnswer(request, key, authentication, tokenConsumer));
  }

  private SseEmitter stream(
      AnswerRequest request,
      String idempotencyKey,
      Authentication authentication,
      StreamExecutor executor) {
    SseEmitter emitter = new SseEmitter(120_000L);
    CompletableFuture.runAsync(
        () -> {
          try {
            AnswerResponse response =
                executor.execute(
                    idempotencyKey, text -> send(emitter, "token", new TokenEvent(text)));
            send(emitter, "complete", response);
            emitter.complete();
          } catch (RuntimeException error) {
            try {
              send(emitter, "error", streamError(error));
              emitter.complete();
            } catch (RuntimeException ignored) {
              emitter.completeWithError(error);
            }
          }
        });
    return emitter;
  }

  private static void send(SseEmitter emitter, String event, Object data) {
    try {
      emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
    } catch (IOException error) {
      throw new StreamDisconnectedException(error);
    }
  }

  private static StreamError streamError(RuntimeException error) {
    if (error instanceof BillingException billing) {
      return new StreamError(
          billing.getCode(), billing.getMessage(), billing.getStatus().is5xxServerError());
    }
    if (error instanceof WebImportConsentRequiredException) {
      return new StreamError("AI_EXTERNAL_CONSENT_REQUIRED", error.getMessage(), false);
    }
    if (error instanceof WebImportBillingRequiredException) {
      return new StreamError("AI_BILLING_REQUIRED", error.getMessage(), false);
    }
    if (error instanceof ExternalSearchUnavailableException) {
      return new StreamError("EXTERNAL_SEARCH_UNAVAILABLE", error.getMessage(), true);
    }
    if (error instanceof WebImportUnavailableException) {
      return new StreamError("WEB_IMPORT_UNAVAILABLE", error.getMessage(), true);
    }
    return new StreamError("CHAT_STREAM_FAILED", "Chat stream did not complete.", true);
  }

  @FunctionalInterface
  private interface StreamExecutor {
    AnswerResponse execute(
        String idempotencyKey, java.util.function.Consumer<String> tokenConsumer);
  }

  private record TokenEvent(String text) {}

  private record StreamError(String code, String message, boolean retryable) {}

  private static final class StreamDisconnectedException extends RuntimeException {
    private StreamDisconnectedException(Throwable cause) {
      super(cause);
    }
  }
}

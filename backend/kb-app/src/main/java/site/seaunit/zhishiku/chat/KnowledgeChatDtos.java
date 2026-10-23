package site.seaunit.zhishiku.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class KnowledgeChatDtos {
  private KnowledgeChatDtos() {}

  public record RetrievalRequest(@NotBlank @Size(max = 300) String query) {
    public RetrievalRequest {
      query = query == null ? null : query.strip();
    }
  }

  public record ChatHistoryMessage(
      @NotBlank
          @Pattern(regexp = "user|assistant", message = "History role must be user or assistant.")
          String role,
      @NotBlank @Size(max = 2_000) String content) {}

  public record AnswerRequest(
      @NotBlank @Size(max = 300) String query,
      @Size(max = 6) List<@NotNull @Valid ChatHistoryMessage> history,
      boolean externalAiConsent,
      UUID billingTaskId) {
    public AnswerRequest {
      query = query == null ? null : query.strip();
      history = history == null ? List.of() : List.copyOf(history);
    }
  }

  public record ChatSource(
      String kind,
      UUID knowledgeId,
      String title,
      String excerpt,
      String url,
      String provider,
      List<String> matchedFields) {
    public ChatSource {
      matchedFields = matchedFields == null ? List.of() : List.copyOf(matchedFields);
    }
  }

  public record RetrievalResponse(boolean hasContext, List<ChatSource> sources) {
    public RetrievalResponse {
      sources = List.copyOf(sources);
    }
  }

  public record AnswerResponse(
      String answer, boolean needsInternet, List<ChatSource> sources, String reason) {
    public AnswerResponse {
      sources = List.copyOf(sources);
    }
  }

  public record CreateConversationRequest(@NotBlank @Size(max = 100) String title) {
    public CreateConversationRequest {
      title = title == null ? null : title.strip();
    }
  }

  public record AppendMessageRequest(
      @NotBlank
          @Pattern(regexp = "user|assistant", message = "Message role must be user or assistant.")
          String role,
      @NotBlank @Size(max = 8_000) String content,
      @NotNull @Size(max = 5) List<@NotNull @Valid ChatSource> sources,
      boolean needsInternet,
      @Size(max = 100) String reason) {
    public AppendMessageRequest {
      content = content == null ? null : content.strip();
      sources = sources == null ? List.of() : List.copyOf(sources);
    }
  }

  public record ChatConversationResponse(
      UUID id,
      String title,
      String lastMessage,
      long messageCount,
      Instant createdAt,
      Instant updatedAt) {}

  public record ChatConversationPage(List<ChatConversationResponse> items) {
    public ChatConversationPage {
      items = List.copyOf(items);
    }
  }

  public record ChatMessageResponse(
      UUID id,
      String role,
      String content,
      List<ChatSource> sources,
      boolean needsInternet,
      String reason,
      Instant createdAt) {
    public ChatMessageResponse {
      sources = List.copyOf(sources);
    }
  }

  public record ChatConversationDetail(
      ChatConversationResponse conversation, List<ChatMessageResponse> messages) {
    public ChatConversationDetail {
      messages = List.copyOf(messages);
    }
  }

  public record ConversationDeleteResponse(boolean deleted) {}
}

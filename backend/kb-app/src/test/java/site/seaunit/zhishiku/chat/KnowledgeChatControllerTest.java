package site.seaunit.zhishiku.chat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.ChatConversationPage;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.ChatConversationResponse;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.ChatMessageResponse;
import site.seaunit.zhishiku.chat.KnowledgeChatDtos.ChatSource;
import site.seaunit.zhishiku.security.SecurityConfig;

@WebMvcTest(KnowledgeChatController.class)
@Import({
  SecurityConfig.class,
  site.seaunit.zhishiku.common.ApiExceptionHandler.class,
  KnowledgeChatExceptionHandler.class
})
class KnowledgeChatControllerTest {
  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper mapper;

  @MockBean private KnowledgeChatService service;
  @MockBean private KnowledgeChatConversationService conversations;
  @MockBean private AuthenticationManager authenticationManager;

  @Test
  void conversationHistoryRequiresAuthentication() throws Exception {
    mvc.perform(get("/api/v1/chat/conversations")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void listsAccountConversationHistory() throws Exception {
    UUID id = UUID.randomUUID();
    when(conversations.list(any(), eq(20)))
        .thenReturn(
            new ChatConversationPage(
                List.of(
                    new ChatConversationResponse(
                        id,
                        "许愿式编程",
                        "最近一条回答",
                        2,
                        Instant.parse("2026-10-23T08:00:00Z"),
                        Instant.parse("2026-10-23T08:01:00Z")))));

    mvc.perform(get("/api/v1/chat/conversations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].title").value("许愿式编程"))
        .andExpect(jsonPath("$.items[0].messageCount").value(2));
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void creatingConversationRequiresCsrfAndIdempotencyKey() throws Exception {
    UUID id = UUID.randomUUID();
    when(conversations.create(any(), any(), eq("chat-create-key-001")))
        .thenReturn(
            new ChatConversationResponse(
                id,
                "新对话",
                "",
                0,
                Instant.parse("2026-10-23T08:00:00Z"),
                Instant.parse("2026-10-23T08:00:00Z")));

    mvc.perform(
            post("/api/v1/chat/conversations")
                .header("Idempotency-Key", "chat-create-key-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"新对话\"}"))
        .andExpect(status().isForbidden());

    mvc.perform(
            post("/api/v1/chat/conversations")
                .with(csrf())
                .header("Idempotency-Key", "chat-create-key-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"新对话\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("新对话"));
  }

  @Test
  @WithMockUser(username = "alice@example.com")
  void persistsAssistantMessagesWithMoreThanFiveCitedKnowledgeItems() throws Exception {
    UUID conversationId = UUID.randomUUID();
    UUID messageId = UUID.randomUUID();
    List<ChatSource> sources =
        IntStream.rangeClosed(1, 8)
            .mapToObj(
                index ->
                    new ChatSource(
                        "knowledge",
                        UUID.randomUUID(),
                        "引用 " + index,
                        "摘要 " + index,
                        "https://example.com/" + index,
                        null,
                        List.of("body")))
            .toList();
    when(conversations.append(any(), eq(conversationId), any(), eq("chat-message-key")))
        .thenReturn(
            new ChatMessageResponse(
                messageId,
                "assistant",
                "基于八条知识的回答",
                sources,
                false,
                null,
                Instant.parse("2026-10-25T11:14:42Z")));

    String request =
        mapper.writeValueAsString(
            Map.of(
                "role",
                "assistant",
                "content",
                "基于八条知识的回答",
                "sources",
                sources,
                "needsInternet",
                false));

    mvc.perform(
            post("/api/v1/chat/conversations/{id}/messages", conversationId)
                .with(csrf())
                .header("Idempotency-Key", "chat-message-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sources.length()").value(8))
        .andExpect(jsonPath("$.content").value("基于八条知识的回答"));
  }
}

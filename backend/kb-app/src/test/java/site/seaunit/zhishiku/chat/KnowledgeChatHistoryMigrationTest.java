package site.seaunit.zhishiku.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class KnowledgeChatHistoryMigrationTest {
  @Test
  void chatHistoryIsAccountScopedAndCascadesWithItsConversation() throws Exception {
    String sql =
        new ClassPathResource("db/migration/V0017__knowledge_chat_history.sql")
            .getContentAsString(StandardCharsets.UTF_8);

    assertThat(sql)
        .contains("CREATE TABLE chat_conversation")
        .contains("CREATE TABLE chat_message")
        .contains("REFERENCES owner_account(id) ON DELETE CASCADE")
        .contains("FOREIGN KEY (conversation_id, owner_id)")
        .contains("ON DELETE CASCADE")
        .contains("chat_message_sources_array_check");
  }

  @Test
  void persistedChatSourcesOnlyAllowHttpLinks() {
    assertThat(KnowledgeChatConversationService.safeHttpUrl("https://example.com/article"))
        .isEqualTo("https://example.com/article");
    assertThat(KnowledgeChatConversationService.safeHttpUrl("javascript:alert(1)")).isNull();
    assertThat(KnowledgeChatConversationService.safeHttpUrl("https://user:pass@example.com"))
        .isNull();
  }
}

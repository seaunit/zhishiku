package site.seaunit.zhishiku.chat;

public class KnowledgeChatConversationNotFoundException extends RuntimeException {
  public KnowledgeChatConversationNotFoundException() {
    super("Chat conversation was not found.");
  }
}

package site.seaunit.zhishiku.chat;

import java.util.List;
import java.util.function.Consumer;

public interface KnowledgeChatModelClient {
  ModelAnswer answer(String question, List<HistoryMessage> history, List<Evidence> evidence);

  ModelAnswer answerGenerally(String question, List<HistoryMessage> history);

  List<String> planQueries(String question);

  String streamAnswer(
      String question,
      List<HistoryMessage> history,
      List<Evidence> evidence,
      Consumer<String> tokenConsumer);

  String streamGeneralAnswer(
      String question, List<HistoryMessage> history, Consumer<String> tokenConsumer);

  record HistoryMessage(String role, String content) {}

  record Evidence(String id, String title, String content, String url) {}

  record ModelAnswer(String answer, List<String> usedSourceIds, boolean needsInternet) {
    public ModelAnswer {
      usedSourceIds = usedSourceIds == null ? List.of() : List.copyOf(usedSourceIds);
    }
  }
}

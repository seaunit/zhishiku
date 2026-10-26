package site.seaunit.zhishiku.search.external;

import java.util.List;

public interface ExternalAnswerService {
  String answer(String query, List<DeepSeekWebProvider.SearchResult> results);
}

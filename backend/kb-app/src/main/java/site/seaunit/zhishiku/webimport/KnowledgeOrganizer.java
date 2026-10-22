package site.seaunit.zhishiku.webimport;

import java.util.List;

public interface KnowledgeOrganizer {
  List<OrganizedKnowledge> organize(String sourceLabel, String text);
}

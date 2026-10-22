package site.seaunit.zhishiku.webimport;

import java.util.List;

public record OrganizedKnowledge(
    String title, String body, String summary, String category, List<String> tags) {}

package site.seaunit.zhishiku.search.richtext;

public record SearchProjection(
    String titleText,
    String bodyText,
    String noteText,
    String imageText,
    String searchText,
    long expectedSearchRevision) {}

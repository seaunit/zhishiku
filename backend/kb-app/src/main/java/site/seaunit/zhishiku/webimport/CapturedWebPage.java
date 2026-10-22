package site.seaunit.zhishiku.webimport;

public record CapturedWebPage(String requestedUrl, String finalUrl, String visibleText) {}

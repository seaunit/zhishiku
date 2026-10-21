package site.seaunit.zhishiku.common;

public record ErrorResponse(String code, String message, String requestId, boolean retryable) {}

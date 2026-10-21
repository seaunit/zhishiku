package site.seaunit.zhishiku.auth;

public record CsrfResponse(String token, String headerName) {}

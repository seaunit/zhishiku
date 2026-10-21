package site.seaunit.zhishiku.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmailCodeResponse(long expiresInSeconds, long resendAfterSeconds, String debugCode) {}

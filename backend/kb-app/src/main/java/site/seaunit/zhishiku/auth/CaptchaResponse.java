package site.seaunit.zhishiku.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CaptchaResponse(
    String captchaId, String image, long expiresInSeconds, String debugAnswer) {}

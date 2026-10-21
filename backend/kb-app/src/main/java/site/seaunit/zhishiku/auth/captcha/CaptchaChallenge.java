package site.seaunit.zhishiku.auth.captcha;

public record CaptchaChallenge(
    String captchaId, String image, long expiresInSeconds, String answer) {}

package site.seaunit.zhishiku.auth.registration;

public record EmailCodeIssued(long expiresInSeconds, long resendAfterSeconds, String code) {}

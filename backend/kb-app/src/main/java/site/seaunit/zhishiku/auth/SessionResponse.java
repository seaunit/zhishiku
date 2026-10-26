package site.seaunit.zhishiku.auth;

import java.util.UUID;

public record SessionResponse(
    UUID ownerId,
    String loginName,
    String email,
    String displayName,
    String locale,
    boolean admin,
    String csrfToken) {}

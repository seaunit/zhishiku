package site.seaunit.zhishiku.auth;

import java.util.UUID;

public record SessionResponse(
    UUID ownerId, String email, String displayName, String locale, String csrfToken) {}

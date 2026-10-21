package site.seaunit.zhishiku.profile;

import jakarta.validation.constraints.NotNull;

public record UpdateProfileRequest(@NotNull String displayName) {}

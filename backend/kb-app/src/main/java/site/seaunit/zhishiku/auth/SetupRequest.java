package site.seaunit.zhishiku.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetupRequest(
    @NotBlank @Size(min = 32, max = 512) String bootstrapToken,
    @Email @NotBlank String email,
    @NotBlank @Size(min = 12, max = 256) String password,
    @NotBlank String locale) {}

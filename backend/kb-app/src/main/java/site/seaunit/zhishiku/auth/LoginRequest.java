package site.seaunit.zhishiku.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Size(min = 5, max = 254) String email,
    @NotBlank @Size(min = 8, max = 256) String password,
    @NotBlank String captchaId,
    @NotBlank @Size(min = 4, max = 8) String captchaCode) {}

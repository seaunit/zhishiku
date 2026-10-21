package site.seaunit.zhishiku.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @Email @NotBlank String email,
    @NotBlank @Size(min = 12, max = 256) String password,
    @NotBlank String captchaId,
    @NotBlank @Size(min = 4, max = 8) String captchaCode) {}

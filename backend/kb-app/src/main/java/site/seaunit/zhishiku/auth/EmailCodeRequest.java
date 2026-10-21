package site.seaunit.zhishiku.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailCodeRequest(
    @Email @NotBlank String email,
    @NotBlank String captchaId,
    @NotBlank @Size(min = 4, max = 8) String captchaCode) {}

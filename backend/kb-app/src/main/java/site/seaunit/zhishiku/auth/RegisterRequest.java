package site.seaunit.zhishiku.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @Email @NotBlank String email,
    @NotBlank @Size(min = 12, max = 256) String password,
    @NotBlank @Size(min = 12, max = 256) String confirmPassword,
    @NotBlank @Pattern(regexp = "\\d{6}") String emailCode,
    @NotBlank @Pattern(regexp = "zh-CN|en") String locale) {}

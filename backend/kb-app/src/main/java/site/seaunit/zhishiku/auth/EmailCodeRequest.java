package site.seaunit.zhishiku.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailCodeRequest(@Email @NotBlank String email) {}

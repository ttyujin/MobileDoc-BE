package com.mobiledoc.mobiledocbackend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordResetConfirmRequest {
    @NotBlank
    public String token;

    @NotBlank
    @Size(min = 4, max = 72)
    public String newPassword;
}

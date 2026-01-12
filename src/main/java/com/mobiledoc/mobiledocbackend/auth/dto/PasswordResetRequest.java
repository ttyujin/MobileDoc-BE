package com.mobiledoc.mobiledocbackend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class PasswordResetRequest {
    @NotBlank
    @Email
    public String email;
}

package com.mobiledoc.mobiledocbackend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class SendEmailCodeRequest {
    @NotBlank @Email
    public String email;

    @NotBlank
    public String purpose; // "SIGNUP" or "RESET_PASSWORD"
}

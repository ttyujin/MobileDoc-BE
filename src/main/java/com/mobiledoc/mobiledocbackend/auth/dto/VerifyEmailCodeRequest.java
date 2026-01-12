package com.mobiledoc.mobiledocbackend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class VerifyEmailCodeRequest {
    @NotBlank @Email
    public String email;

    @NotBlank
    public String purpose;

    @NotBlank
    public String code;
}

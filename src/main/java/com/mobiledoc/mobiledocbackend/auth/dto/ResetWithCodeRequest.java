package com.mobiledoc.mobiledocbackend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ResetWithCodeRequest {
    @NotBlank @Email
    public String email;

    @NotBlank
    public String code;

    @NotBlank
    @Size(min = 8, max = 72, message = "비밀번호는 8자 이상이어야 해요.")
    @Pattern(
            regexp = "^(?=.*[^a-zA-Z0-9\\s]).{8,72}$",
            message = "비밀번호는 8자 이상이고 특수문자(공백 제외) 1개 이상을 포함해야 해요."
    )
    public String newPassword;
}


package com.mobiledoc.mobiledocbackend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SignupRequest {

    @NotBlank(message = "이름을 입력해주세요.")
    public String name;

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "이메일 형식이 올바르지 않아요.")
    public String email;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, max = 72, message = "비밀번호는 8자 이상이어야 해요.")
    @Pattern(
            regexp = "^(?=.*[^a-zA-Z0-9]).{8,72}$",
            message = "비밀번호는 8자 이상이고 특수문자(영문/숫자 제외) 1개 이상을 포함해야 해요."
    )
    public String password;

}

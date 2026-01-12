package com.mobiledoc.mobiledocbackend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class FindEmailRequest {
    @NotBlank
    public String name;
}

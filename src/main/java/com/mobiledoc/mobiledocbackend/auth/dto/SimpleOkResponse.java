package com.mobiledoc.mobiledocbackend.auth.dto;

public class SimpleOkResponse {
    public boolean ok;
    public String message;

    public SimpleOkResponse(boolean ok, String message) {
        this.ok = ok;
        this.message = message;
    }
}

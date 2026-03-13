package com.mobiledoc.mobiledocbackend.alerts.dto;

public class ChecklistSummaryEmailResponse {
    public boolean ok;
    public String message;

    public ChecklistSummaryEmailResponse() {}

    public ChecklistSummaryEmailResponse(boolean ok, String message) {
        this.ok = ok;
        this.message = message;
    }
}
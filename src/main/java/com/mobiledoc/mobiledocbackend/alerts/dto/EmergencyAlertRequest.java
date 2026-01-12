package com.mobiledoc.mobiledocbackend.alerts.dto;

public class EmergencyAlertRequest {
    private Reporter reporter;
    private Target target;
    private String message;

    public Reporter getReporter() { return reporter; }
    public void setReporter(Reporter reporter) { this.reporter = reporter; }

    public Target getTarget() { return target; }
    public void setTarget(Target target) { this.target = target; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static class Reporter {
        private String userId;
        private String name;
        private String email;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class Target {
        private String contactId;
        private String name;
        private String email;
        private String phone;

        public String getContactId() { return contactId; }
        public void setContactId(String contactId) { this.contactId = contactId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }
}

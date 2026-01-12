package com.mobiledoc.mobiledocbackend.profile.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ProfileResponse {

    public UUID userId;

    public String sido;
    public String detailRegion;
    public String meds;
    public String frequentHospital;
    public String conditions;
    public String allergies;

    public String pickupPreference;
    public String patientType;
    public String emergencyContact;

    public String notes;
    public Instant updatedAt;

    public List<ContactItem> contacts;
    public List<VisitItem> visitHistory;

    public static class ContactItem {
        public String id;
        public String name;
        public String relation;
        public String email;
        public String phone;
        public String memo;
    }

    public static class VisitItem {
        public String id;
        public String date;
        public String hospital;
        public String diagnosis;
    }
}

package com.mobiledoc.mobiledocbackend.profile.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class ProfileUpsertRequest {

    @NotBlank
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

    public List<ContactDto> contacts = new ArrayList<>();
    public List<VisitDto> visitHistory = new ArrayList<>();

    @NoArgsConstructor
    public static class ContactDto {
        public String name;
        public String relation;
        public String email;
        public String phone;
        public String memo;
    }

    @NoArgsConstructor
    public static class VisitDto {
        public String date; // "YYYY-MM-DD"
        public String hospital;
        public String diagnosis;
    }
}

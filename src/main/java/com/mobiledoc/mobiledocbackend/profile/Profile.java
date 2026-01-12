package com.mobiledoc.mobiledocbackend.profile;

import com.mobiledoc.mobiledocbackend.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

@Getter
@NoArgsConstructor(access = PROTECTED)
@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String sido;

    private String detailRegion;

    @Column(columnDefinition = "TEXT")
    private String meds;

    @Column(columnDefinition = "TEXT")
    private String frequentHospital;

    @Column(columnDefinition = "TEXT")
    private String conditions;

    @Column(columnDefinition = "TEXT")
    private String allergies;

    private String pickupPreference;
    private String patientType;
    private String emergencyContact;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public Profile(User user, String sido) {
        this.user = user;
        this.sido = sido;
        this.updatedAt = Instant.now();
    }

    public void updateFrom(
            String sido,
            String detailRegion,
            String meds,
            String frequentHospital,
            String conditions,
            String allergies,
            String pickupPreference,
            String patientType,
            String emergencyContact,
            String notes
    ) {
        this.sido = sido;
        this.detailRegion = detailRegion;
        this.meds = meds;
        this.frequentHospital = frequentHospital;
        this.conditions = conditions;
        this.allergies = allergies;
        this.pickupPreference = pickupPreference;
        this.patientType = patientType;
        this.emergencyContact = emergencyContact;
        this.notes = notes;
        this.updatedAt = Instant.now();
    }
}

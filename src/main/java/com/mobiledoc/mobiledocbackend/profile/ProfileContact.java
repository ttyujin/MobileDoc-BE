package com.mobiledoc.mobiledocbackend.profile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

@Getter
@NoArgsConstructor(access = PROTECTED)
@Entity
@Table(name = "profile_contacts")
public class ProfileContact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    private String relation;
    private String email;
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public ProfileContact(UUID userId, String name, String relation, String email, String phone, String memo) {
        this.userId = userId;
        this.name = name;
        this.relation = relation;
        this.email = email;
        this.phone = phone;
        this.memo = memo;
        this.createdAt = Instant.now();
    }
}

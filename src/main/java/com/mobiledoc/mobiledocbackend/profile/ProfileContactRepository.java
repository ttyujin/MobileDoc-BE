package com.mobiledoc.mobiledocbackend.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProfileContactRepository extends JpaRepository<ProfileContact, UUID> {
    List<ProfileContact> findAllByUserIdOrderByCreatedAtAsc(UUID userId);
    void deleteAllByUserId(UUID userId);
}

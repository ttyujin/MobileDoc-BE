package com.mobiledoc.mobiledocbackend.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProfileVisitHistoryRepository extends JpaRepository<ProfileVisitHistory, UUID> {
    List<ProfileVisitHistory> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
    void deleteAllByUserId(UUID userId);
}

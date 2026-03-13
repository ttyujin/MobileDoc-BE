// src/main/java/com/mobiledoc/mobiledocbackend/stats/SymptomLogRepository.java
package com.mobiledoc.mobiledocbackend.stats;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SymptomLogRepository extends JpaRepository<SymptomLog, Long> {

    List<SymptomLog> findByUserEmailAndCreatedAtAfterOrderByCreatedAtDesc(String userEmail, LocalDateTime from);
}
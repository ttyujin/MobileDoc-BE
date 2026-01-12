package com.mobiledoc.mobiledocbackend.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);

    // ✅ 아이디(이메일) 찾기용
    List<User> findByNameIgnoreCase(String name);
}

package com.mobiledoc.mobiledocbackend.profile;

import com.mobiledoc.mobiledocbackend.profile.dto.ProfileUpsertRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> get(@PathVariable UUID userId) {
        return ResponseEntity.ok(profileService.get(userId)); // 없으면 null
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> upsert(@PathVariable UUID userId, @Valid @RequestBody ProfileUpsertRequest req) {
        return ResponseEntity.ok(profileService.upsert(userId, req));
    }
}

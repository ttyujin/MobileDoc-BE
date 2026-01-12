package com.mobiledoc.mobiledocbackend.profile;

import com.mobiledoc.mobiledocbackend.profile.dto.ProfileResponse;
import com.mobiledoc.mobiledocbackend.profile.dto.ProfileUpsertRequest;
import com.mobiledoc.mobiledocbackend.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final ProfileContactRepository contactRepository;
    private final ProfileVisitHistoryRepository visitRepository;
    private final UserRepository userRepository;

    public ProfileService(ProfileRepository profileRepository,
                          ProfileContactRepository contactRepository,
                          ProfileVisitHistoryRepository visitRepository,
                          UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.contactRepository = contactRepository;
        this.visitRepository = visitRepository;
        this.userRepository = userRepository;
    }

    public ProfileResponse get(UUID userId) {
        var profileOpt = profileRepository.findById(userId);
        if (profileOpt.isEmpty()) return null;

        var profile = profileOpt.get();
        var contacts = contactRepository.findAllByUserIdOrderByCreatedAtAsc(userId);
        var visits = visitRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

        return Mapper.toResponse(profile, contacts, visits);
    }

    @Transactional
    public ProfileResponse upsert(UUID userId, ProfileUpsertRequest req) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않아."));

        var profile = profileRepository.findById(userId)
                .orElseGet(() -> new Profile(user, req.sido));

        profile.updateFrom(
                req.sido,
                req.detailRegion,
                req.meds,
                req.frequentHospital,
                req.conditions,
                req.allergies,
                req.pickupPreference,
                req.patientType,
                req.emergencyContact,
                req.notes
        );
        profileRepository.save(profile);

        // ✅ 덮어쓰기(프론트 상태 = 최신)
        contactRepository.deleteAllByUserId(userId);
        for (var c : req.contacts) {
            if (c == null) continue;
            String name = c.name == null ? "" : c.name.trim();
            if (name.isEmpty()) continue;
            contactRepository.save(new ProfileContact(
                    userId, name, c.relation, c.email, c.phone, c.memo
            ));
        }

        visitRepository.deleteAllByUserId(userId);
        for (var v : req.visitHistory) {
            if (v == null) continue;

            LocalDate date = null;
            if (v.date != null && !v.date.isBlank()) date = LocalDate.parse(v.date.trim());

            boolean hasAny = (v.hospital != null && !v.hospital.isBlank())
                    || (v.diagnosis != null && !v.diagnosis.isBlank())
                    || date != null;
            if (!hasAny) continue;

            visitRepository.save(new ProfileVisitHistory(userId, date, v.hospital, v.diagnosis));
        }

        var contacts = contactRepository.findAllByUserIdOrderByCreatedAtAsc(userId);
        var visits = visitRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        return Mapper.toResponse(profile, contacts, visits);
    }

    static class Mapper {
        static ProfileResponse toResponse(Profile p,
                                          java.util.List<ProfileContact> contacts,
                                          java.util.List<ProfileVisitHistory> visits) {
            ProfileResponse r = new ProfileResponse();

            r.userId = p.getUserId();
            r.sido = p.getSido();
            r.detailRegion = p.getDetailRegion();
            r.meds = p.getMeds();
            r.frequentHospital = p.getFrequentHospital();
            r.conditions = p.getConditions();
            r.allergies = p.getAllergies();
            r.pickupPreference = p.getPickupPreference();
            r.patientType = p.getPatientType();
            r.emergencyContact = p.getEmergencyContact();
            r.notes = p.getNotes();
            r.updatedAt = p.getUpdatedAt();

            r.contacts = contacts.stream().map(c -> {
                ProfileResponse.ContactItem i = new ProfileResponse.ContactItem();
                i.id = c.getId().toString();
                i.name = c.getName();
                i.relation = c.getRelation();
                i.email = c.getEmail();
                i.phone = c.getPhone();
                i.memo = c.getMemo();
                return i;
            }).toList();

            r.visitHistory = visits.stream().map(v -> {
                ProfileResponse.VisitItem i = new ProfileResponse.VisitItem();
                i.id = v.getId().toString();
                i.date = v.getDate() == null ? "" : v.getDate().toString();
                i.hospital = v.getHospital();
                i.diagnosis = v.getDiagnosis();
                return i;
            }).toList();

            return r;
        }
    }
}

package com.careeragent.api;

import com.careeragent.api.dto.PreferenceResponse;
import com.careeragent.api.dto.ProfileResponse;
import com.careeragent.api.dto.UpdatePreferenceRequest;
import com.careeragent.api.dto.UpdateProfileRequest;
import com.careeragent.domain.CandidatePreference;
import com.careeragent.domain.CandidateProfile;
import com.careeragent.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.UUID;

/**
 * REST endpoints for candidate profile and preference management.
 */
@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * Returns the authenticated candidate's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getProfile() {
        CandidateProfile profile = profileService.getProfile(getCurrentCandidateId());
        return ResponseEntity.ok(toProfileResponse(profile));
    }

    /**
     * Updates the authenticated candidate's profile fields.
     */
    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        CandidateProfile profile = profileService.updateProfile(getCurrentCandidateId(), request);
        return ResponseEntity.ok(toProfileResponse(profile));
    }

    /**
     * Returns the authenticated candidate's job search preferences.
     */
    @GetMapping("/me/preferences")
    public ResponseEntity<PreferenceResponse> getPreferences() {
        CandidatePreference pref = profileService.getPreferences(getCurrentCandidateId());
        if (pref == null) {
            return ResponseEntity.ok(emptyPreferenceResponse());
        }
        return ResponseEntity.ok(toPreferenceResponse(pref));
    }

    /**
     * Updates the authenticated candidate's job search preferences.
     */
    @PutMapping("/me/preferences")
    public ResponseEntity<PreferenceResponse> updatePreferences(
            @Valid @RequestBody UpdatePreferenceRequest request) {
        CandidatePreference pref = profileService.updatePreferences(getCurrentCandidateId(), request);
        return ResponseEntity.ok(toPreferenceResponse(pref));
    }

    /**
     * Deletes the authenticated candidate's profile and all associated data.
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteProfile() {
        profileService.deleteProfile(getCurrentCandidateId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Extracts the candidate ID from the current security context.
     */
    private UUID getCurrentCandidateId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (UUID) auth.getPrincipal();
    }

    /**
     * Returns an empty preference response for candidates without preferences.
     */
    private PreferenceResponse emptyPreferenceResponse() {
        return new PreferenceResponse(
                null, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), null, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Converts a CandidateProfile entity to a ProfileResponse DTO.
     */
    private ProfileResponse toProfileResponse(CandidateProfile profile) {
        return new ProfileResponse(
                profile.getId(),
                profile.getEmail(),
                profile.getName(),
                profile.getPhone(),
                profile.getSummary(),
                profile.getApplicationMode().name(),
                profile.getPreSubmitReview().name(),
                profile.getMatchScoreThreshold(),
                profile.getTimezone(),
                profile.getActive(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    /**
     * Converts a CandidatePreference entity to a PreferenceResponse DTO.
     */
    private PreferenceResponse toPreferenceResponse(CandidatePreference pref) {
        return new PreferenceResponse(
                pref.getId(),
                pref.getTargetJobTitles(),
                pref.getPreferredLocations(),
                pref.getRemotePreferences(),
                pref.getMinSalary(),
                pref.getPreferredIndustries(),
                pref.getTargetCompanies(),
                pref.getSeniorityLevels(),
                pref.getMustHaveRequirements(),
                pref.getExclusions()
        );
    }
}

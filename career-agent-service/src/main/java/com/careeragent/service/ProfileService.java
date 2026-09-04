package com.careeragent.service;

import com.careeragent.api.dto.UpdatePreferenceRequest;
import com.careeragent.api.dto.UpdateProfileRequest;
import com.careeragent.api.exception.ResourceNotFoundException;
import com.careeragent.domain.CandidatePreference;
import com.careeragent.domain.CandidateProfile;
import com.careeragent.integration.storage.ObjectStorageService;
import com.careeragent.repository.CandidateDocumentRepository;
import com.careeragent.repository.CandidatePreferenceRepository;
import com.careeragent.repository.CandidateProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.UUID;

/**
 * Manages candidate profile CRUD, preferences, activation, and deletion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final CandidateProfileRepository profileRepository;
    private final CandidatePreferenceRepository preferenceRepository;
    private final CandidateDocumentRepository documentRepository;
    private final ValidationService validationService;
    private final ObjectStorageService storageService;

    /**
     * Returns the profile for the given candidate ID.
     */
    public CandidateProfile getProfile(UUID candidateId) {
        return profileRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
    }

    /**
     * Updates the profile fields (name, phone, summary) with input sanitization.
     */
    public CandidateProfile updateProfile(UUID candidateId, UpdateProfileRequest request) {
        CandidateProfile profile = getProfile(candidateId);

        if (request.name() != null) {
            profile.setName(validationService.sanitizeText(request.name()));
        }
        if (request.phone() != null) {
            profile.setPhone(validationService.sanitizeText(request.phone()));
        }
        if (request.summary() != null) {
            profile.setSummary(validationService.sanitizeText(request.summary()));
        }

        return profileRepository.save(profile);
    }

    /**
     * Returns the job search preferences for the given candidate.
     */
    public CandidatePreference getPreferences(UUID candidateId) {
        if (!profileRepository.existsById(candidateId)) {
            throw new ResourceNotFoundException("Profile not found");
        }
        return preferenceRepository.findByCandidateId(candidateId).orElse(null);
    }

    /**
     * Updates preferences with validation, sanitization, and profile activation check.
     */
    @Transactional
    public CandidatePreference updatePreferences(UUID candidateId, UpdatePreferenceRequest request) {
        CandidateProfile profile = getProfile(candidateId);

        var remotePreferences = validationService.validateRemotePreferences(request.remotePreferences());
        var seniorityLevels = validationService.validateSeniorityLevels(request.seniorityLevels());

        CandidatePreference pref = preferenceRepository.findByCandidateId(candidateId)
                .orElseGet(() -> {
                    CandidatePreference newPref = new CandidatePreference();
                    newPref.setCandidate(profile);
                    return newPref;
                });

        pref.setTargetJobTitles(
                request.targetJobTitles() != null
                        ? validationService.sanitizeList(request.targetJobTitles())
                        : Collections.emptyList());
        pref.setPreferredLocations(
                request.preferredLocations() != null
                        ? validationService.sanitizeList(request.preferredLocations())
                        : Collections.emptyList());
        pref.setRemotePreferences(remotePreferences);
        pref.setMinSalary(request.minSalary());
        pref.setPreferredIndustries(
                request.preferredIndustries() != null
                        ? validationService.sanitizeList(request.preferredIndustries())
                        : Collections.emptyList());
        pref.setTargetCompanies(
                request.targetCompanies() != null
                        ? validationService.sanitizeList(request.targetCompanies())
                        : Collections.emptyList());
        pref.setSeniorityLevels(seniorityLevels);
        pref.setMustHaveRequirements(
                request.mustHaveRequirements() != null
                        ? validationService.sanitizeList(request.mustHaveRequirements())
                        : Collections.emptyList());
        pref.setExclusions(
                request.exclusions() != null
                        ? validationService.sanitizeList(request.exclusions())
                        : Collections.emptyList());

        pref = preferenceRepository.save(pref);

        // Profile activation check: ≥1 target job title AND ≥1 preferred location
        boolean canActivate = pref.getTargetJobTitles() != null && !pref.getTargetJobTitles().isEmpty()
                && pref.getPreferredLocations() != null && !pref.getPreferredLocations().isEmpty();
        profile.setActive(canActivate);
        profileRepository.save(profile);

        return pref;
    }

    /**
     * Deletes the candidate's profile, preferences, documents, and stored files.
     */
    @Transactional
    public void deleteProfile(UUID candidateId) {
        if (!profileRepository.existsById(candidateId)) {
            throw new ResourceNotFoundException("Profile not found");
        }

        storageService.deleteAllForCandidate(candidateId);
        documentRepository.deleteByCandidateId(candidateId);
        preferenceRepository.deleteByCandidateId(candidateId);
        profileRepository.deleteById(candidateId);
    }
}

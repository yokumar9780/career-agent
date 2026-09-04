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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProfileService covering profile CRUD, preferences, and deletion.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileService — Requirements 1.1, 2.1, 2.2, 2.3, 2.4")
class ProfileServiceTest {

    @Mock private CandidateProfileRepository profileRepository;
    @Mock private CandidatePreferenceRepository preferenceRepository;
    @Mock private CandidateDocumentRepository documentRepository;
    @Mock private ValidationService validationService;
    @Mock private ObjectStorageService storageService;

    @InjectMocks private ProfileService profileService;

    private CandidateProfile buildProfile(UUID id) {
        return CandidateProfile.builder()
                .id(id)
                .email("test@example.com")
                .passwordHash("hashed")
                .name("Test User")
                .build();
    }

    // --- getProfile ---

    @Test
    @DisplayName("getProfile returns profile when found")
    void getProfile_found_returnsProfile() {
        UUID id = UUID.randomUUID();
        CandidateProfile profile = buildProfile(id);
        when(profileRepository.findById(id)).thenReturn(Optional.of(profile));

        CandidateProfile result = profileService.getProfile(id);

        assertThat(result).isEqualTo(profile);
        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("getProfile throws ResourceNotFoundException when not found")
    void getProfile_notFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(profileRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfile(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Profile not found");
    }

    // --- updateProfile ---

    @Test
    @DisplayName("updateProfile sanitizes all text fields (name, phone, summary)")
    void updateProfile_sanitizesAllTextFields() {
        UUID id = UUID.randomUUID();
        CandidateProfile profile = buildProfile(id);
        when(profileRepository.findById(id)).thenReturn(Optional.of(profile));
        when(validationService.sanitizeText("New Name")).thenReturn("New Name");
        when(validationService.sanitizeText("+1234567890")).thenReturn("+1234567890");
        when(validationService.sanitizeText("My summary")).thenReturn("My summary");
        when(profileRepository.save(any(CandidateProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new UpdateProfileRequest("New Name", "+1234567890", "My summary");
        CandidateProfile result = profileService.updateProfile(id, request);

        verify(validationService).sanitizeText("New Name");
        verify(validationService).sanitizeText("+1234567890");
        verify(validationService).sanitizeText("My summary");
        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getPhone()).isEqualTo("+1234567890");
        assertThat(result.getSummary()).isEqualTo("My summary");
    }

    @Test
    @DisplayName("updateProfile ignores null fields (partial update)")
    void updateProfile_nullFields_ignoresNullFields() {
        UUID id = UUID.randomUUID();
        CandidateProfile profile = buildProfile(id);
        profile.setPhone("original-phone");
        profile.setSummary("original-summary");
        when(profileRepository.findById(id)).thenReturn(Optional.of(profile));
        when(validationService.sanitizeText("Updated Name")).thenReturn("Updated Name");
        when(profileRepository.save(any(CandidateProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new UpdateProfileRequest("Updated Name", null, null);
        CandidateProfile result = profileService.updateProfile(id, request);

        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getPhone()).isEqualTo("original-phone");
        assertThat(result.getSummary()).isEqualTo("original-summary");
        verify(validationService, times(1)).sanitizeText(any());
    }

    // --- getPreferences ---

    @Test
    @DisplayName("getPreferences returns null for candidate with no preferences")
    void getPreferences_noPreferences_returnsNull() {
        UUID id = UUID.randomUUID();
        when(profileRepository.existsById(id)).thenReturn(true);
        when(preferenceRepository.findByCandidateId(id)).thenReturn(Optional.empty());

        CandidatePreference result = profileService.getPreferences(id);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getPreferences throws ResourceNotFoundException for non-existent profile")
    void getPreferences_profileNotFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(profileRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> profileService.getPreferences(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Profile not found");
    }

    // --- updatePreferences ---

    @Test
    @DisplayName("updatePreferences creates new preference when none exists")
    void updatePreferences_noExisting_createsNewPreference() {
        UUID id = UUID.randomUUID();
        CandidateProfile profile = buildProfile(id);
        when(profileRepository.findById(id)).thenReturn(Optional.of(profile));
        when(validationService.validateRemotePreferences(any())).thenReturn(Collections.emptyList());
        when(validationService.validateSeniorityLevels(any())).thenReturn(Collections.emptyList());
        when(validationService.sanitizeList(any())).thenAnswer(inv -> inv.getArgument(0));
        when(preferenceRepository.findByCandidateId(id)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(CandidatePreference.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileRepository.save(any(CandidateProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new UpdatePreferenceRequest(
                List.of("Engineer"), List.of("NYC"), null, null, null, null, null, null, null);

        CandidatePreference result = profileService.updatePreferences(id, request);

        assertThat(result).isNotNull();
        assertThat(result.getCandidate()).isEqualTo(profile);
        verify(preferenceRepository).save(any(CandidatePreference.class));
    }

    @Test
    @DisplayName("updatePreferences updates existing preference")
    void updatePreferences_existing_updatesPreference() {
        UUID id = UUID.randomUUID();
        CandidateProfile profile = buildProfile(id);
        CandidatePreference existingPref = new CandidatePreference();
        existingPref.setCandidate(profile);
        existingPref.setTargetJobTitles(List.of("Old Title"));

        when(profileRepository.findById(id)).thenReturn(Optional.of(profile));
        when(validationService.validateRemotePreferences(any())).thenReturn(Collections.emptyList());
        when(validationService.validateSeniorityLevels(any())).thenReturn(Collections.emptyList());
        when(validationService.sanitizeList(any())).thenAnswer(inv -> inv.getArgument(0));
        when(preferenceRepository.findByCandidateId(id)).thenReturn(Optional.of(existingPref));
        when(preferenceRepository.save(any(CandidatePreference.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileRepository.save(any(CandidateProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new UpdatePreferenceRequest(
                List.of("New Title"), List.of("London"), null, null, null, null, null, null, null);

        CandidatePreference result = profileService.updatePreferences(id, request);

        assertThat(result.getTargetJobTitles()).containsExactly("New Title");
        assertThat(result.getPreferredLocations()).containsExactly("London");
    }

    @Test
    @DisplayName("updatePreferences activates profile when ≥1 job title AND ≥1 location")
    void updatePreferences_withTitleAndLocation_activatesProfile() {
        UUID id = UUID.randomUUID();
        CandidateProfile profile = buildProfile(id);
        profile.setActive(false);

        when(profileRepository.findById(id)).thenReturn(Optional.of(profile));
        when(validationService.validateRemotePreferences(any())).thenReturn(Collections.emptyList());
        when(validationService.validateSeniorityLevels(any())).thenReturn(Collections.emptyList());
        when(validationService.sanitizeList(any())).thenAnswer(inv -> inv.getArgument(0));
        when(preferenceRepository.findByCandidateId(id)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(CandidatePreference.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileRepository.save(any(CandidateProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new UpdatePreferenceRequest(
                List.of("Engineer"), List.of("NYC"), null, null, null, null, null, null, null);

        profileService.updatePreferences(id, request);

        assertThat(profile.getActive()).isTrue();
    }

    @Test
    @DisplayName("updatePreferences deactivates profile when job titles empty")
    void updatePreferences_emptyJobTitles_deactivatesProfile() {
        UUID id = UUID.randomUUID();
        CandidateProfile profile = buildProfile(id);
        profile.setActive(true);

        when(profileRepository.findById(id)).thenReturn(Optional.of(profile));
        when(validationService.validateRemotePreferences(any())).thenReturn(Collections.emptyList());
        when(validationService.validateSeniorityLevels(any())).thenReturn(Collections.emptyList());
        when(validationService.sanitizeList(any())).thenAnswer(inv -> inv.getArgument(0));
        when(preferenceRepository.findByCandidateId(id)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(CandidatePreference.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileRepository.save(any(CandidateProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new UpdatePreferenceRequest(
                Collections.emptyList(), List.of("NYC"), null, null, null, null, null, null, null);

        profileService.updatePreferences(id, request);

        assertThat(profile.getActive()).isFalse();
    }

    @Test
    @DisplayName("updatePreferences deactivates profile when locations empty")
    void updatePreferences_emptyLocations_deactivatesProfile() {
        UUID id = UUID.randomUUID();
        CandidateProfile profile = buildProfile(id);
        profile.setActive(true);

        when(profileRepository.findById(id)).thenReturn(Optional.of(profile));
        when(validationService.validateRemotePreferences(any())).thenReturn(Collections.emptyList());
        when(validationService.validateSeniorityLevels(any())).thenReturn(Collections.emptyList());
        when(validationService.sanitizeList(any())).thenAnswer(inv -> inv.getArgument(0));
        when(preferenceRepository.findByCandidateId(id)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(CandidatePreference.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileRepository.save(any(CandidateProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new UpdatePreferenceRequest(
                List.of("Engineer"), Collections.emptyList(), null, null, null, null, null, null, null);

        profileService.updatePreferences(id, request);

        assertThat(profile.getActive()).isFalse();
    }

    @Test
    @DisplayName("updatePreferences validates remote preferences (invalid value throws IllegalArgumentException)")
    void updatePreferences_invalidRemotePreference_throwsIllegalArgumentException() {
        UUID id = UUID.randomUUID();
        CandidateProfile profile = buildProfile(id);
        when(profileRepository.findById(id)).thenReturn(Optional.of(profile));
        when(validationService.validateRemotePreferences(List.of("INVALID")))
                .thenThrow(new IllegalArgumentException("Invalid remote preference: 'INVALID'"));

        var request = new UpdatePreferenceRequest(
                List.of("Engineer"), List.of("NYC"), List.of("INVALID"), null, null, null, null, null, null);

        assertThatThrownBy(() -> profileService.updatePreferences(id, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid remote preference");
    }

    @Test
    @DisplayName("updatePreferences validates seniority levels (invalid value throws IllegalArgumentException)")
    void updatePreferences_invalidSeniorityLevel_throwsIllegalArgumentException() {
        UUID id = UUID.randomUUID();
        CandidateProfile profile = buildProfile(id);
        when(profileRepository.findById(id)).thenReturn(Optional.of(profile));
        when(validationService.validateRemotePreferences(any())).thenReturn(Collections.emptyList());
        when(validationService.validateSeniorityLevels(List.of("INVALID")))
                .thenThrow(new IllegalArgumentException("Invalid seniority level: 'INVALID'"));

        var request = new UpdatePreferenceRequest(
                List.of("Engineer"), List.of("NYC"), null, null, null, null, List.of("INVALID"), null, null);

        assertThatThrownBy(() -> profileService.updatePreferences(id, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid seniority level");
    }

    // --- deleteProfile ---

    @Test
    @DisplayName("deleteProfile deletes all associated data (storage, documents, preferences, profile)")
    void deleteProfile_existingProfile_deletesAllData() {
        UUID id = UUID.randomUUID();
        when(profileRepository.existsById(id)).thenReturn(true);

        profileService.deleteProfile(id);

        verify(storageService).deleteAllForCandidate(id);
        verify(documentRepository).deleteByCandidateId(id);
        verify(preferenceRepository).deleteByCandidateId(id);
        verify(profileRepository).deleteById(id);
    }

    @Test
    @DisplayName("deleteProfile throws ResourceNotFoundException when profile doesn't exist")
    void deleteProfile_notFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(profileRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> profileService.deleteProfile(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Profile not found");
    }
}

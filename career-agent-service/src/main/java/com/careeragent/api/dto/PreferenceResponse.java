package com.careeragent.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PreferenceResponse(
        UUID id,
        List<String> targetJobTitles,
        List<String> preferredLocations,
        List<String> remotePreferences,
        BigDecimal minSalary,
        List<String> preferredIndustries,
        List<String> targetCompanies,
        List<String> seniorityLevels,
        List<String> mustHaveRequirements,
        List<String> exclusions
) {
}

package com.careeragent.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UpdatePreferenceRequest(
        @Size(max = 20, message = "Maximum 20 target job titles allowed")
        List<@NotBlank @Size(max = 300) String> targetJobTitles,

        @Size(max = 20, message = "Maximum 20 preferred locations allowed")
        List<@NotBlank @Size(max = 200, message = "Location must be at most 200 characters") String> preferredLocations,

        List<@Size(max = 20) String> remotePreferences,

        @DecimalMin(value = "0.01", message = "Salary must be at least 0.01")
        @DecimalMax(value = "999999999.99", message = "Salary must be at most 999,999,999.99")
        BigDecimal minSalary,

        List<@Size(max = 200) String> preferredIndustries,

        List<@Size(max = 200) String> targetCompanies,

        List<@Size(max = 20) String> seniorityLevels,

        List<@Size(max = 500) String> mustHaveRequirements,

        List<@Size(max = 500) String> exclusions
) {
}

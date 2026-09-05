package com.careeragent.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for email-based job ingestion.
 */
@Configuration
@ConfigurationProperties(prefix = "email.ingestion")
@Getter
@Setter
public class EmailIngestionConfig {

    private boolean enabled;
    private String folder;
    private int maxEmailsPerRun;
    private String linkedinAlertSender;
    private int lookbackDays;
}

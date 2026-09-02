# Requirements Document

## Introduction

The Career Agent is an AI-powered job search assistant that automatically discovers relevant job postings, scores them against a candidate's profile and preferences, prepares tailored application packages, and lets the candidate approve what gets applied to. The MVP delivers a production-grade, customer-testable system covering the end-to-end workflow: profile creation, job ingestion from two sources (LinkedIn email alerts and company career pages), AI-powered matching, a shortlist dashboard, and application preparation with full user control. No application is ever submitted without explicit user action.

The system is designed with a pluggable Job_Portal architecture. LinkedIn is the first supported portal for the MVP, with a Portal_Adapter pattern that abstracts job ingestion, session management, and application submission. This architecture enables additional job portals (Indeed, Greenhouse, Lever, company career portals, etc.) to be integrated in future releases without modifying core application logic.

## Glossary

- **Career_Agent**: The overall system comprising backend services (Spring Boot), AI agents, and a React frontend that orchestrates the job discovery, matching, and application preparation workflow.
- **Candidate_Profile**: A structured representation of a job seeker's professional background, skills, experience, and job search preferences stored in the database.
- **Profile_Agent**: An AI agent that converts uploaded CVs and user-provided preferences into a structured Candidate_Profile.
- **Job**: A normalized representation of a job posting containing title, company, location, remote type, salary range, description, requirements, skills, URL, source, and posted date.
- **Job_Ingestion_Service**: The backend service responsible for collecting raw job postings from configured sources on a scheduled basis.
- **Email_Listener**: A Spring Boot service that connects to a configured email inbox and parses LinkedIn Job Alert emails to extract job posting data.
- **Career_Page_Scraper**: A service that fetches and parses job listings from a configurable list of target company career pages.
- **Job_Analysis_Agent**: An AI agent that normalizes raw job posting data into the standard Job schema, extracting structured requirements and skills.
- **Deduplication_Service**: A service that identifies and consolidates duplicate job postings from multiple sources using company name, normalized title, location, and URL/source matching.
- **Matching_Agent**: An AI agent that evaluates a Job against a Candidate_Profile and produces a MatchResult with dimensional scores and a recommendation.
- **MatchResult**: The output of the Matching_Agent containing an overall score (0–100), a recommendation (APPLY or SKIP), dimension scores (skills, experience, location, salary, seniority), strengths, gaps, and risks.
- **Shortlist_Dashboard**: The React frontend view that displays matched jobs ranked by score with filtering, detail viewing, and action capabilities.
- **Application_Agent**: An AI agent that generates a tailored application package (CV recommendation, cover letter, screening question answers) for a shortlisted job.
- **Application_Package**: The collection of documents (CV recommendation, cover letter, screening question answers) prepared by the Application_Agent for a specific job.
- **Workflow_Execution**: A record of a scheduled or manually triggered end-to-end processing run tracking its status, timing, and results.
- **LLM_Provider**: The large language model service accessed via Spring AI ChatClient for powering the four AI agents.
- **Application_Mode**: A per-candidate configuration setting that determines how approved applications are submitted. Values are MANUAL (candidate applies externally and marks as applied) or AUTO_APPLY (the system submits the application via browser automation).
- **Job_Portal**: A generic abstraction representing any external job platform (LinkedIn, Indeed, Greenhouse, Lever, etc.) that the system can integrate with for job discovery, application submission, or both.
- **Portal_Adapter**: A pluggable component that implements the Job_Portal interface for a specific platform, handling platform-specific authentication, job ingestion, form filling, and submission logic.
- **Job_Source_Adapter**: A pluggable component that implements job ingestion from a specific source type (email alerts, career page scraping, API-based feeds, etc.).
- **Browser_Automation_Service**: A separate service using Playwright that automates browser interactions for submitting job applications across supported job portals via portal-specific submission handlers, operating outside the main Spring Boot application boundary. LinkedIn is the first supported portal.
- **LinkedIn_Easy_Apply**: LinkedIn's streamlined application flow that allows candidates to apply directly on LinkedIn without being redirected to an external company website.
- **Application_Submission**: The automated process of navigating to a job's application page on the originating portal, filling in form fields from the approved Application_Package, uploading the CV, answering screening questions, and submitting the application. The specific interaction flow is handled by the appropriate Portal_Adapter.
- **Pre_Submit_Review**: An optional confirmation gate where the system pauses before final submission and presents a summary to the candidate for approval, even in AUTO_APPLY mode.

## Requirements

### Requirement 1: Candidate Profile Creation

**User Story:** As a candidate, I want to create my professional profile from my CV and preferences, so that the system can match me with relevant job opportunities.

#### Acceptance Criteria

1. WHEN a candidate uploads a CV document (PDF or DOCX format, maximum 10 MB), THE Profile_Agent SHALL extract professional information and produce a structured Candidate_Profile within 60 seconds.
2. WHEN a candidate provides job search preferences (target job titles, preferred locations, remote/hybrid/on-site preference, minimum salary, preferred industries, target companies, seniority level, must-have requirements, and exclusions), THE Career_Agent SHALL store the preferences as part of the Candidate_Profile.
3. WHEN the Profile_Agent completes CV extraction, THE Career_Agent SHALL present the extracted profile to the candidate for review and editing before saving.
4. THE Career_Agent SHALL validate that a Candidate_Profile contains at least one target job title and one preferred location before marking the profile as active.
5. WHEN a candidate updates any field of the Candidate_Profile, THE Career_Agent SHALL persist the changes and use the updated profile for all subsequent matching operations.
6. IF the Profile_Agent fails to extract data from an uploaded CV, THEN THE Career_Agent SHALL display an error message indicating the extraction failure reason and retain the uploaded document for manual profile entry.
7. THE Career_Agent SHALL store uploaded CV documents in the configured document storage (local filesystem or S3-compatible storage).

### Requirement 2: Candidate Profile Management

**User Story:** As a candidate, I want to view, edit, and manage my profile, so that my job preferences stay current and accurate.

#### Acceptance Criteria

1. THE Career_Agent SHALL provide API endpoints for creating, reading, updating, and deleting a Candidate_Profile.
2. WHEN a candidate updates job search preferences, THE Career_Agent SHALL validate all fields (salary as a positive number between 0.01 and 999,999,999.99, seniority level as one of "intern", "junior", "mid", "senior", "lead", or "executive", locations as non-empty strings with a maximum length of 200 characters each and a maximum of 20 entries) before persisting changes.
3. THE Career_Agent SHALL allow a candidate to upload, replace, and delete CV documents associated with the Candidate_Profile, accepting files in PDF, DOC, or DOCX format, with a maximum file size of 5 MB per document and a maximum of 5 documents per profile.
4. WHEN a candidate deletes a Candidate_Profile, THE Career_Agent SHALL remove all associated preferences, documents, and stored files.
5. IF a candidate submits profile or preference data that fails validation, THEN THE Career_Agent SHALL reject the request, return an error message indicating each field that failed validation and the reason, and preserve the previously stored data unchanged.
6. IF a request targets a Candidate_Profile that does not belong to the authenticated candidate, THEN THE Career_Agent SHALL reject the request with an error message indicating insufficient permissions and leave the target profile unchanged.

### Requirement 3: Job Ingestion from LinkedIn Email Alerts

**User Story:** As a candidate, I want the system to automatically collect jobs from my LinkedIn job alert emails, so that I receive relevant opportunities without manual searching.

**Note:** This requirement is implemented as the LinkedIn email Job_Source_Adapter, one of multiple pluggable source adapters in the ingestion pipeline. The Job_Ingestion_Service processes jobs from all registered Job_Source_Adapters using a uniform interface.

#### Acceptance Criteria

1. WHEN the Job_Ingestion_Service runs on its configured schedule (default: daily at 08:00), THE Email_Listener SHALL connect to the configured email inbox and retrieve up to 200 unprocessed LinkedIn Job Alert emails per run.
2. WHEN the Email_Listener retrieves a LinkedIn Job Alert email, THE Email_Listener SHALL parse the email content and extract individual job posting data including job title, company name, location, and job URL, treating any missing field other than job URL as empty.
3. IF a job posting cannot be extracted because the job URL is missing or unparseable, THEN THE Email_Listener SHALL skip that posting, log a warning identifying the email subject and the position of the failed posting, and continue extracting remaining postings from the same email.
4. WHEN the Email_Listener successfully extracts at least one job posting from an email, THE Email_Listener SHALL store each extracted job posting in the Career_Agent data store and mark the source email as processed to prevent reprocessing on subsequent runs.
5. IF a job posting has the same job URL as an existing record in the Career_Agent data store, THEN THE Email_Listener SHALL skip the duplicate posting without creating a new record.
6. IF the Email_Listener fails to connect to the configured email inbox, THEN THE Career_Agent SHALL log the connection failure with the error details and retry on the next scheduled run.
7. IF the Email_Listener encounters an email with an unrecognized format that yields zero extractable job postings, THEN THE Career_Agent SHALL log a warning with the email subject and sender, mark the email as processed, and continue processing remaining emails.
8. THE Email_Listener SHALL support IMAP-based email inbox connections configured via application properties (host, port, username, credentials, folder).

### Requirement 4: Job Ingestion from Company Career Pages

**User Story:** As a candidate, I want the system to check career pages of my target companies, so that I catch job postings that may not appear in LinkedIn alerts.

**Note:** This requirement is implemented as the career page Job_Source_Adapter, one of multiple pluggable source adapters in the ingestion pipeline. The Job_Ingestion_Service processes jobs from all registered Job_Source_Adapters using a uniform interface.

#### Acceptance Criteria

1. WHEN the Job_Ingestion_Service runs on its configured schedule, THE Career_Page_Scraper SHALL fetch job listings from each company career page URL in the candidate's configured target company list.
2. WHEN the Career_Page_Scraper fetches a career page, THE Career_Page_Scraper SHALL extract the following data for each listing: job title, company name, location, and job URL.
3. IF a job posting has the same job URL as an existing record in the Career_Agent data store, THEN THE Career_Page_Scraper SHALL skip the duplicate posting without creating a new record.
4. THE Career_Agent SHALL provide a configuration interface for the candidate to add, edit, and remove target company career page URLs, with a maximum of 50 target company URLs per candidate.
5. IF the Career_Page_Scraper fails to fetch a specific company career page (HTTP error, timeout after 30 seconds, or connection failure), THEN THE Career_Agent SHALL log the failure with the company name and error details, skip the failed page, and continue processing remaining pages.
6. WHEN a new company career page URL is added, THE Career_Agent SHALL validate that the URL is no longer than 2048 characters, conforms to valid URL syntax, and uses the HTTPS protocol before saving.
7. IF a company career page URL fails validation, THEN THE Career_Agent SHALL reject the addition and return an error message indicating which validation rule was violated.

### Requirement 5: Job Normalization

**User Story:** As a candidate, I want all job postings normalized to a consistent format regardless of source, so that I can compare opportunities fairly.

#### Acceptance Criteria

1. WHEN raw job posting data is ingested from any source, THE Job_Analysis_Agent SHALL normalize the data into the standard Job schema containing all of the following fields: title, company, location, remoteType, salaryRange, description, requirements, skills, url, source, and postedDate, with no fields omitted, within 30 seconds per job.
2. WHEN the Job_Analysis_Agent normalizes a job posting, THE Job_Analysis_Agent SHALL classify the remoteType as one of: REMOTE, HYBRID, ON_SITE, or UNSPECIFIED.
3. WHEN the Job_Analysis_Agent normalizes a job posting, THE Job_Analysis_Agent SHALL extract a list of up to 50 individual skills and qualifications from the job description, each represented as a separate text entry of no more than 100 characters.
4. IF the Job_Analysis_Agent cannot determine a value for a required field (salaryRange, location, or remoteType), THEN THE Job_Analysis_Agent SHALL set the field to an UNSPECIFIED indicator value appropriate to the field type rather than omitting the field.
5. THE Job_Analysis_Agent SHALL preserve the original source URL, source identifier, and portal identifier for each normalized Job. Source identifiers SHALL include the portal name as a prefix (e.g., LINKEDIN_EMAIL, LINKEDIN_CAREER_PAGE, INDEED_API) and support arbitrary portal identifiers registered through the Job_Portal abstraction layer.
6. WHEN the Job_Analysis_Agent normalizes a job posting, THE Job_Analysis_Agent SHALL parse and store the posting date as a standard ISO 8601 date; IF the posting date cannot be determined, THEN THE Job_Analysis_Agent SHALL use the ingestion date.
7. IF the Job_Analysis_Agent ingests a job posting that matches an already-normalized job from a different source based on matching title and company, THEN THE Job_Analysis_Agent SHALL retain the existing normalized record and append the new source URL, source identifier, and portal identifier rather than creating a duplicate entry.

### Requirement 6: Duplicate Detection

**User Story:** As a candidate, I want duplicate job postings from different sources merged into a single entry, so that my shortlist is clean and accurate.

#### Acceptance Criteria

1. WHEN a new Job is normalized, THE Deduplication_Service SHALL check for existing jobs matching on the exact combination of company name (case-insensitive), normalized job title (case-insensitive), and location (case-insensitive).
2. WHEN the Deduplication_Service identifies a duplicate, THE Deduplication_Service SHALL merge the new source information into the existing Job record rather than creating a new record.
3. WHEN the Deduplication_Service merges a duplicate, THE Deduplication_Service SHALL retain all distinct source URLs and source identifiers on the merged Job record.
4. WHEN the Deduplication_Service merges a duplicate, THE Deduplication_Service SHALL use the data from the more recently ingested source for fields that differ between sources.
5. THE Deduplication_Service SHALL log each detected duplicate with the matched job ID and the source of the new duplicate entry.
6. IF no existing job matches the normalized company name, title, and location combination, THEN THE Deduplication_Service SHALL create a new Job record.

### Requirement 7: AI Job Matching

**User Story:** As a candidate, I want every job scored against my profile with a clear explanation, so that I can focus on the best opportunities.

#### Acceptance Criteria

1. WHEN a normalized Job is ready for matching, THE Matching_Agent SHALL evaluate the Job against the active Candidate_Profile and produce a MatchResult within 15 seconds.
2. THE Matching_Agent SHALL produce a MatchResult containing: an overall score (integer 0–100), a recommendation (APPLY or SKIP), and dimension scores for skills match, experience match, location match, salary match, and seniority match (each integer 0–100), where the overall score is a weighted combination of the dimension scores.
3. THE Matching_Agent SHALL include in the MatchResult: a list of 1 to 10 strengths (profile advantages for the role), a list of 0 to 10 gaps (profile shortcomings for the role), and a list of 0 to 10 risks (potential concerns about fit).
4. WHEN the MatchResult overall score is greater than or equal to the configured threshold (default: 60), THE Career_Agent SHALL set the Job status to SHORTLISTED.
5. WHEN the MatchResult overall score is less than the configured threshold, THE Career_Agent SHALL set the Job status to SKIPPED.
6. IF the Matching_Agent fails to produce a MatchResult (LLM timeout or error) after exhausting retries, THEN THE Career_Agent SHALL log the failure with the job ID and error details, set the Job status to ANALYZED (pending retry), and continue processing remaining jobs.
7. THE Career_Agent SHALL allow the candidate to configure the match score threshold as an integer between 1 and 100 via the application settings.

### Requirement 8: Shortlist Dashboard

**User Story:** As a candidate, I want a dashboard showing my top-matched jobs with scores and explanations, so that I can quickly decide which ones to pursue.

#### Acceptance Criteria

1. THE Shortlist_Dashboard SHALL display a list of shortlisted jobs ranked by match score in descending order, showing job title, company name, location, remote type, match score, and a textual match summary derived from the MatchResult for each job.
2. THE Shortlist_Dashboard SHALL provide filters for: minimum match score, location, company name, job title keyword, job status, and date range (based on ingestion date).
3. WHEN a candidate selects a job from the shortlist, THE Shortlist_Dashboard SHALL display the full job details including the complete description, the MatchResult with all dimension scores, strengths, gaps, and risks.
4. THE Shortlist_Dashboard SHALL provide action buttons for each shortlisted job: "Prepare Application" (to trigger application package generation), "Skip" (to mark the job as SKIPPED), and "View Details" (to open the full job detail view).
5. WHEN a candidate clicks "Skip" on a shortlisted job, THE Career_Agent SHALL update the job status to SKIPPED and remove the job from the default shortlist view.
6. THE Shortlist_Dashboard SHALL display a summary count of: total jobs ingested, jobs shortlisted, jobs skipped, and applications in progress.
7. THE Shortlist_Dashboard SHALL support pagination with a configurable page size between 1 and 100 (default: 20 jobs per page).
8. WHEN a candidate views skipped jobs, THE Shortlist_Dashboard SHALL provide a "Restore" action that moves the job back to SHORTLISTED status.
9. WHEN no jobs match the applied filters or no shortlisted jobs exist, THE Shortlist_Dashboard SHALL display a message indicating that no jobs are available with the current filters.

### Requirement 9: Application Package Preparation

**User Story:** As a candidate, I want the system to generate a tailored application package for a selected job, so that I can apply with high-quality, customized materials.

#### Acceptance Criteria

1. WHEN a candidate requests application preparation for a shortlisted job that has an existing MatchResult, THE Application_Agent SHALL generate an Application_Package containing: a CV recommendation (suggestions for tailoring the CV to the role), a customized cover letter, and answers to up to 5 common screening questions relevant to the job's role and industry, within 90 seconds.
2. WHEN generating a cover letter for an Application_Package, THE Application_Agent SHALL tailor the cover letter to highlight the candidate's strengths identified in the MatchResult and address the gaps identified in the MatchResult.
3. WHEN the Application_Agent completes the Application_Package, THE Career_Agent SHALL present all generated materials (CV recommendation, cover letter, and screening question answers) to the candidate for review and editing through the Shortlist_Dashboard before any further action.
4. WHILE the Application_Package is in review, THE Career_Agent SHALL allow the candidate to edit the generated cover letter, CV recommendations, and screening question answers through the Shortlist_Dashboard.
5. WHEN a candidate approves the Application_Package, THE Career_Agent SHALL update the job status to READY_TO_APPLY.
6. IF the Application_Agent fails to generate an Application_Package due to a timeout exceeding 90 seconds or an upstream service error, THEN THE Career_Agent SHALL display an error message indicating that package generation failed and allow the candidate to retry the preparation.
7. WHEN the Application_Agent generates an Application_Package or the candidate saves edits to an Application_Package, THE Career_Agent SHALL store the Application_Package and all candidate edits as ApplicationDocument records associated with the Application.
8. IF a candidate requests application preparation for a job that does not have a MatchResult or is not in shortlisted status, THEN THE Career_Agent SHALL display an error message indicating that the job must be shortlisted with a completed match analysis before application preparation can begin.

### Requirement 10: Application Tracking

**User Story:** As a candidate, I want to track the status of every job through the entire lifecycle, so that I have a clear view of my job search progress.

#### Acceptance Criteria

1. THE Career_Agent SHALL track each job through the following primary status sequence: NEW → ANALYZED → MATCHED → SHORTLISTED → APPLICATION_PREPARED → READY_TO_APPLY → APPLIED → INTERVIEW → OFFER → CLOSED. WHEN Application_Mode is AUTO_APPLY, THE Career_Agent SHALL transition a job from READY_TO_APPLY to APPLIED automatically upon successful browser submission by the Browser_Automation_Service rather than requiring manual candidate action.
2. THE Career_Agent SHALL support the following side statuses: REJECTED (from APPLIED, INTERVIEW, or OFFER), SKIPPED (from SHORTLISTED or MATCHED), EXPIRED (from any status before APPLIED), and SUBMISSION_FAILED (from READY_TO_APPLY when auto-apply fails), where side statuses are terminal unless the candidate explicitly moves the job back to a primary status.
3. WHEN a candidate marks a job as "Applied", THE Career_Agent SHALL record the applied date, the CV version used, and the cover letter version used, and update the job status to APPLIED.
4. THE Career_Agent SHALL provide an application tracking view showing all tracked applications with: company name, position title, applied date (blank if not yet applied), match score, current status, and days since last status change, sorted by most recent status change first.
5. WHEN a candidate updates the status of an application, THE Career_Agent SHALL record every status change with a timestamp.
6. IF a candidate attempts an invalid status transition, THEN THE Career_Agent SHALL reject the change with an error message indicating the current status and the set of valid next statuses.

### Requirement 11: Scheduled Workflow Execution

**User Story:** As a candidate, I want the job discovery and matching workflow to run automatically on a schedule, so that I receive fresh opportunities daily without manual intervention.

#### Acceptance Criteria

1. THE Career_Agent SHALL execute the full workflow (ingest → normalize → deduplicate → match → shortlist) on a configurable schedule (default: daily at 08:00 in the candidate's configured timezone) with a minimum schedule interval of 1 hour.
2. WHEN a scheduled workflow execution starts, THE Career_Agent SHALL create a Workflow_Execution record with a start timestamp and status RUNNING.
3. WHEN a scheduled workflow execution completes, THE Career_Agent SHALL update the Workflow_Execution record with an end timestamp, status (COMPLETED or FAILED), count of jobs ingested, count of duplicates detected, count of jobs matched, and count of jobs shortlisted.
4. IF a scheduled workflow execution fails at any stage, THEN THE Career_Agent SHALL log the failure details, update the Workflow_Execution record with status FAILED and the error description, skip the failed stage, and continue processing subsequent stages.
5. WHEN a candidate triggers the workflow manually through the Shortlist_Dashboard, THE Career_Agent SHALL execute the same workflow and track it with a Workflow_Execution record.
6. IF a candidate triggers a manual workflow execution while another workflow execution is already running, THEN THE Career_Agent SHALL reject the manual trigger and display a message on the Shortlist_Dashboard indicating that a workflow is already in progress.
7. IF the candidate has not configured a timezone, THEN THE Career_Agent SHALL default to 08:00 UTC for scheduled workflow execution.

### Requirement 12: Authentication and Authorization

**User Story:** As a candidate, I want my data protected by authentication, so that only I can access my profile, jobs, and applications.

#### Acceptance Criteria

1. THE Career_Agent SHALL require authentication for all API endpoints except the login and registration endpoints.
2. WHEN a candidate submits valid login credentials, THE Career_Agent SHALL issue a JWT access token with a configurable expiration (default: 24 hours).
3. WHEN a request includes an expired or invalid JWT token, THE Career_Agent SHALL reject the request with HTTP status 401 and an error message indicating whether the token is expired, malformed, or missing.
4. THE Career_Agent SHALL ensure that each authenticated candidate can access only their own profile, jobs, matches, and applications.
5. IF an authenticated candidate attempts to access a resource belonging to a different candidate, THEN THE Career_Agent SHALL reject the request with HTTP status 403.
6. WHEN a candidate registers a new account, THE Career_Agent SHALL validate that the email address is unique and the password meets minimum complexity requirements (8 to 128 characters, at least one uppercase letter, one lowercase letter, and one digit).
7. THE Career_Agent SHALL hash all stored passwords using bcrypt with a minimum cost factor of 10.

### Requirement 13: Input Validation and Error Handling

**User Story:** As a candidate, I want the system to validate my input and return clear error messages, so that I can correct mistakes without confusion.

#### Acceptance Criteria

1. THE Career_Agent SHALL validate all API request payloads against their defined schemas and reject invalid requests with HTTP status 400 and a response body listing each validation error with the field name and the violation description.
2. IF an unhandled exception occurs during API request processing, THEN THE Career_Agent SHALL return HTTP status 500 with a generic error message that does not expose internal system details, and log the full exception details including stack trace, request path, and timestamp.
3. THE Career_Agent SHALL return all API error responses in a consistent JSON format containing: timestamp, HTTP status code, error type, message, and request path.
4. THE Career_Agent SHALL strip HTML tags and script content from all user-provided text inputs (profile fields, company names, notes) and persist only the resulting plain text.
5. THE Career_Agent SHALL enforce maximum length constraints on all text input fields (profile name: 200 characters, job title: 300 characters, free-text fields: 5000 characters) and reject inputs exceeding the maximum with a validation error specifying the field name, maximum allowed length, and actual input length.
6. IF an API request body is malformed (not valid JSON), THEN THE Career_Agent SHALL return HTTP status 400 with an error message indicating that the request body could not be parsed.

### Requirement 14: LLM Integration and Resilience

**User Story:** As a candidate, I want the AI features to work reliably and recover gracefully from failures, so that my job search is not disrupted by transient LLM issues.

#### Acceptance Criteria

1. THE Career_Agent SHALL access the LLM_Provider exclusively through the Spring AI ChatClient abstraction to maintain provider independence.
2. WHEN an LLM call fails due to a transient error (HTTP 429, 500, 502, 503), THE Career_Agent SHALL retry the call with exponential backoff up to 3 retries with initial delay of 2 seconds.
3. IF all 3 retries for an LLM call are exhausted without success, THEN THE Career_Agent SHALL mark the originating task as failed, log the final error with the agent name and job ID, and continue processing remaining tasks.
4. THE Career_Agent SHALL enforce a rate limit on LLM calls (configurable, default: 30 calls per minute) to prevent exceeding provider quotas.
5. WHEN the LLM rate limit is reached, THE Career_Agent SHALL queue pending LLM requests up to a maximum queue size of 500 and process them as capacity becomes available.
6. IF the LLM request queue reaches maximum capacity, THEN THE Career_Agent SHALL reject new LLM requests with a queue-full error until capacity becomes available.
7. THE Career_Agent SHALL enforce a timeout of 120 seconds for each individual LLM call and treat timeouts as transient failures eligible for retry.
8. THE Career_Agent SHALL log every LLM call with: the agent name, input token count, output token count, latency in milliseconds, and success or failure status.

### Requirement 15: Logging and Observability

**User Story:** As an operator, I want structured logging and health monitoring, so that I can troubleshoot issues and monitor system health.

#### Acceptance Criteria

1. THE Career_Agent SHALL emit structured JSON log entries containing: timestamp (ISO 8601), log level (one of TRACE, DEBUG, INFO, WARN, ERROR), logger name, message, and correlation ID for request tracing.
2. THE Career_Agent SHALL assign a unique correlation ID (UUID v4) to each incoming API request and propagate the correlation ID through all log entries and downstream service calls within that request.
3. THE Career_Agent SHALL expose a Spring Boot Actuator health endpoint at `/actuator/health` that reports the status of: the database connection, the email inbox connection, and the LLM_Provider availability, where each dependency reports a status of UP or DOWN and the overall status is DOWN if any dependency is DOWN.
4. IF a health check for any dependency does not respond within 5 seconds, THEN THE Career_Agent SHALL report that dependency's status as DOWN.
5. THE Career_Agent SHALL expose a Spring Boot Actuator metrics endpoint at `/actuator/metrics` that reports: total jobs ingested, total jobs matched, total applications prepared, active workflow execution status, and LLM call counts.
6. WHEN a workflow execution completes, THE Career_Agent SHALL log a summary entry at INFO level with: execution ID, duration in milliseconds, jobs ingested count, duplicates detected count, jobs matched count, and jobs shortlisted count.
7. IF an unhandled exception occurs during request processing, THEN THE Career_Agent SHALL log an ERROR-level entry containing the correlation ID, the exception type, and the exception message.

### Requirement 16: Database Schema and Migrations

**User Story:** As a developer, I want database schema changes managed through versioned migrations, so that deployments are repeatable and safe.

#### Acceptance Criteria

1. THE Career_Agent SHALL manage all database schema changes through Flyway versioned migration scripts following the naming convention V{version}__{description}.sql.
2. THE Career_Agent SHALL define the following core database entities: CandidateProfile, CandidatePreference, CandidateDocument, Job, JobMatch, Application, ApplicationDocument, and WorkflowExecution.
3. WHEN the Career_Agent starts, THE Career_Agent SHALL automatically apply any pending Flyway migration scripts before accepting requests, completing within 120 seconds.
4. IF a Flyway migration fails during startup, THEN THE Career_Agent SHALL log the migration error with the failed script name and prevent the application from accepting requests.
5. THE Career_Agent SHALL use PostgreSQL as the sole supported relational database.
6. THE Career_Agent SHALL not support backward (undo) migrations; all schema changes shall be forward-only through new versioned migration scripts.

### Requirement 17: Deployment Configuration

**User Story:** As an operator, I want the system packaged as Docker containers with documented configuration, so that I can deploy and operate the system reliably.

#### Acceptance Criteria

1. THE Career_Agent SHALL provide a Dockerfile for the Spring Boot backend that produces a container image based on a Java 21 runtime.
2. THE Career_Agent SHALL provide a Dockerfile for the React frontend that produces a container image serving the built static assets via a web server.
3. THE Career_Agent SHALL provide a Docker Compose configuration that orchestrates the backend, frontend, and PostgreSQL database containers with correct dependency ordering, health checks that verify each service is ready to accept connections, and exposed port mappings.
4. THE Career_Agent SHALL externalize all environment-specific configuration (database credentials, email credentials, LLM API keys, schedule expressions, rate limits) as environment variables or mounted configuration files.
5. IF a required environment variable or configuration value is missing at startup, THEN THE Career_Agent SHALL log an error identifying the missing configuration key and prevent the application from starting.
6. THE Career_Agent SHALL document all required environment variables with descriptions, default values, and example values in a README or environment template file.
7. THE Career_Agent SHALL persist PostgreSQL data to a named Docker volume so that data survives container restarts.

### Requirement 18: API Design

**User Story:** As a frontend developer, I want a well-structured REST API, so that I can build the dashboard and integrate all features reliably.

#### Acceptance Criteria

1. THE Career_Agent SHALL expose RESTful API endpoints following consistent URL naming conventions using plural resource nouns and path-based resource identification (e.g., `/api/v1/jobs/{id}`, `/api/v1/applications/{id}`).
2. THE Career_Agent SHALL version all API endpoints under the `/api/v1` path prefix.
3. THE Career_Agent SHALL return appropriate HTTP status codes for all operations: 200 for successful retrieval, 201 for successful creation, 204 for successful deletion, 400 for validation errors, 401 for authentication failures, 403 for authorization failures, 404 for resource not found, 409 for conflict, and 500 for unhandled server errors.
4. THE Career_Agent SHALL support CORS configuration allowing the operator to specify allowed origins, allowed HTTP methods, and allowed headers via environment variables or configuration files.
5. WHEN returning paginated results, THE Career_Agent SHALL include pagination metadata (current page number, page size, total elements, and total pages), use a default page size of 20, enforce a maximum page size of 100, and return the first page when no pagination parameters are provided.
6. THE Career_Agent SHALL return all error responses in a consistent JSON structure containing: timestamp (ISO 8601), status (HTTP status code), error (error type string), message (human-readable description), and path (request URI).

### Requirement 19: Application Mode Configuration

**User Story:** As a candidate, I want to choose between manually applying to jobs or having the system auto-apply on LinkedIn, so that I can control my preferred level of automation.

#### Acceptance Criteria

1. THE Career_Agent SHALL provide an Application_Mode setting on the Candidate_Profile with two values: MANUAL (default) and AUTO_APPLY.
2. WHEN a candidate sets Application_Mode to MANUAL, THE Career_Agent SHALL follow the existing manual application flow where the candidate applies externally and marks the job as APPLIED.
3. WHILE Application_Mode is set to AUTO_APPLY for a given job, WHEN the candidate approves the Application_Package, THE Career_Agent SHALL trigger the Browser_Automation_Service to submit the approved Application_Package on LinkedIn within 60 seconds of approval.
4. THE Career_Agent SHALL allow the candidate to override the global Application_Mode on a per-job basis; individual jobs without an explicit override SHALL inherit the current global Application_Mode.
5. WHEN a candidate changes the Application_Mode, THE Career_Agent SHALL apply the new mode only to future application submissions and SHALL NOT change the mode for applications already in READY_TO_APPLY or later statuses.
6. IF the Browser_Automation_Service fails to submit an application during AUTO_APPLY, THEN THE Career_Agent SHALL notify the candidate with an error message indicating the submission failure, retain the approved Application_Package, and set the job's application status back to READY_TO_APPLY so the candidate can retry or switch to MANUAL.
7. WHEN the Browser_Automation_Service successfully submits an application during AUTO_APPLY, THE Career_Agent SHALL notify the candidate that the application has been submitted and update the job's application status to APPLIED.

### Requirement 20: LinkedIn Browser Automation Service

**User Story:** As a candidate using auto-apply, I want the system to automate the LinkedIn application process using my approved materials, so that I can apply to jobs without manual form-filling.

#### Acceptance Criteria

1. THE Browser_Automation_Service SHALL operate as a separate service from the Spring Boot backend, communicating via a REST API or MCP protocol.
2. THE Browser_Automation_Service SHALL use Playwright to automate browser interactions with job application pages across supported portals, with LinkedIn as the first supported portal. The Browser_Automation_Service SHALL operate in headless mode by default, with an option to run in headed mode for debugging.
3. THE Browser_Automation_Service SHALL support portal-specific application flows, starting with LinkedIn Easy Apply, including: single-page applications, multi-step application forms (up to 10 steps), file upload fields (PDF and DOCX), text input fields, dropdown selections, and checkbox/radio button selections.
4. WHEN the Browser_Automation_Service receives an application submission request, THE Browser_Automation_Service SHALL route the request to the appropriate Portal_Adapter based on the job's portal identifier, navigate to the job's application URL within 30 seconds, detect whether the portal supports direct application or redirects to an external site, and proceed only with direct in-portal applications.
5. IF a job application redirects to an external company website (not Easy Apply), THEN THE Browser_Automation_Service SHALL report the job as EXTERNAL_REDIRECT, and THE Career_Agent SHALL fall back to MANUAL mode for that specific job and notify the candidate.
6. THE Browser_Automation_Service SHALL fill application form fields using data from the approved Application_Package: upload the recommended CV document, enter the cover letter text where applicable, and populate screening question answers by matching question text to the Application_Package answers.
7. IF the Browser_Automation_Service encounters a screening question not covered by the Application_Package, THEN THE Browser_Automation_Service SHALL pause the submission, report the unmatched question text to THE Career_Agent, and THE Career_Agent SHALL notify the candidate to provide the missing answer before retrying.
8. THE Browser_Automation_Service SHALL enforce a maximum of 10 application submissions per hour and a maximum of 30 application submissions per day per candidate to reduce the risk of LinkedIn account restrictions.
9. THE Browser_Automation_Service SHALL introduce a randomized delay between 30 and 90 seconds between consecutive application submissions to mimic human behavior.
10. THE Browser_Automation_Service SHALL expose a health endpoint that reports: service availability, current Playwright browser status (running or stopped), and count of submissions in the current hour and day.
11. THE Browser_Automation_Service SHALL support a plugin architecture where new portal submission handlers can be added as separate modules without modifying the core service logic.

### Requirement 21: Portal Session Management

**User Story:** As a candidate using auto-apply, I want the system to securely manage my session for each job portal, so that automated applications work reliably without repeatedly requiring my credentials.

#### Acceptance Criteria

1. THE Career_Agent SHALL provide a mechanism for the candidate to authenticate with a supported Job_Portal through a candidate-initiated browser-based login flow, storing the resulting session cookies in encrypted storage keyed by the combination of candidate ID and portal identifier, with a maximum retention period of 30 days, after which the stored session data SHALL be automatically deleted and the candidate prompted to re-authenticate. LinkedIn is the first supported portal for session management.
2. WHEN the Browser_Automation_Service initiates an application submission for a specific portal, THE Browser_Automation_Service SHALL load the stored session for that portal and candidate, validate the session against the target portal before proceeding, and use the stored session cookies to authenticate without requiring the candidate to re-enter credentials.
3. WHEN a portal session expires or becomes invalid, THE Browser_Automation_Service SHALL detect the session failure within the current or next application submission attempt, pause all pending application submissions for that portal, preserve the state of any in-progress application so it can be retried after re-authentication, and notify the candidate via an in-app notification and email that re-authentication is required for the specific portal.
4. THE Career_Agent SHALL NOT store portal usernames and passwords; only session tokens or cookies obtained through candidate-initiated authentication shall be stored.
5. THE Career_Agent SHALL encrypt all stored portal session data at rest using AES-256 encryption.
6. WHEN a candidate revokes access for a specific portal or deletes their account, THE Career_Agent SHALL delete all stored session data for that portal (or all portals on account deletion) within 5 seconds of detecting the revocation or account deletion event.
7. THE Career_Agent SHALL store each portal session as a separate record identified by candidate ID and portal identifier, allowing the candidate to maintain concurrent active sessions across multiple portals.

### Requirement 22: Pre-Submit Review Gate

**User Story:** As a candidate using auto-apply, I want an optional review step before the system submits each application, so that I can verify the submission details when I want extra control.

#### Acceptance Criteria

1. THE Career_Agent SHALL provide a Pre_Submit_Review setting on the Candidate_Profile with two values: ENABLED (default) and DISABLED.
2. WHEN Pre_Submit_Review is ENABLED and Application_Mode is AUTO_APPLY, THE Career_Agent SHALL pause the submission after the Browser_Automation_Service fills all form fields but before clicking the final submit button, and present a submission summary to the candidate via the Shortlist_Dashboard.
3. THE submission summary SHALL include: the job title, company name, the CV being uploaded, the cover letter content, all screening question answers as filled in the form, and a screenshot of the filled application form.
4. WHEN the candidate approves the pre-submit review, THE Browser_Automation_Service SHALL click the final submit button to complete the application within 30 seconds of approval.
5. WHEN the candidate rejects the pre-submit review, THE Browser_Automation_Service SHALL cancel the submission without clicking submit, and THE Career_Agent SHALL return the job to READY_TO_APPLY status for the candidate to edit the Application_Package.
6. IF the candidate does not respond to a pre-submit review within 24 hours, THEN THE Career_Agent SHALL cancel the submission, close the browser session for that application, and return the job to READY_TO_APPLY status.
7. WHEN Pre_Submit_Review is DISABLED and Application_Mode is AUTO_APPLY, THE Browser_Automation_Service SHALL submit the application immediately after filling all form fields without pausing for review.
8. WHEN a pre-submit review is pending, THE Career_Agent SHALL display a notification badge on the Shortlist_Dashboard indicating the number of applications awaiting review.
9. IF the browser session becomes stale during a pending pre-submit review (page timeout or LinkedIn session expiry), THEN THE Browser_Automation_Service SHALL cancel the current submission, notify the candidate, and allow retry from READY_TO_APPLY status.

### Requirement 23: Application Submission Result Handling

**User Story:** As a candidate, I want the system to detect and record whether each auto-applied application succeeded or failed, so that I have accurate tracking of my applications.

#### Acceptance Criteria

1. WHEN the Browser_Automation_Service submits an application, THE Browser_Automation_Service SHALL detect the submission result within 30 seconds by checking for LinkedIn's confirmation message or page indicating successful submission, where detection succeeds if a confirmation element is found and fails if no confirmation element appears within the 30-second window.
2. WHEN the Browser_Automation_Service detects a successful submission, THE Career_Agent SHALL update the job status to APPLIED, record the submission timestamp, and store a screenshot of the confirmation page as an ApplicationDocument.
3. IF the Browser_Automation_Service fails to submit an application (form validation error, page timeout after 60 seconds, CAPTCHA challenge, or unexpected page structure), THEN THE Career_Agent SHALL update the job status to SUBMISSION_FAILED, log the failure reason, store a screenshot of the failure state, and notify the candidate within 60 seconds of detecting the failure.
4. WHEN a job has status SUBMISSION_FAILED, THE Career_Agent SHALL allow the candidate to retry the auto-apply submission up to a maximum of 3 retry attempts or switch to MANUAL mode for that job.
5. IF the Browser_Automation_Service encounters a CAPTCHA challenge during application submission, THEN THE Browser_Automation_Service SHALL pause the submission, notify the candidate to solve the CAPTCHA manually, and wait up to 5 minutes for the candidate to complete the CAPTCHA before resuming submission.
6. IF the candidate does not complete the CAPTCHA within 5 minutes, THEN THE Career_Agent SHALL treat the submission as failed, update the job status to SUBMISSION_FAILED, and log the failure reason as CAPTCHA timeout.
7. THE Career_Agent SHALL add SUBMISSION_FAILED as a valid side status in the job status model, settable from READY_TO_APPLY status, with valid transitions to READY_TO_APPLY (for retry) or MANUAL mode fallback.
8. THE Career_Agent SHALL record every auto-apply attempt (successful or failed) with: timestamp, job ID, submission method (AUTO_APPLY), result (SUCCESS or FAILED), failure reason if applicable, and screenshot document reference.
9. IF a job reaches the maximum of 3 failed auto-apply retry attempts, THEN THE Career_Agent SHALL prevent further auto-apply retries for that job and notify the candidate to switch to MANUAL mode.

### Requirement 24: Job Portal Abstraction Layer

**User Story:** As a system architect, I want the job portal integration to use a pluggable adapter pattern, so that new job portals can be added without modifying core application logic.

#### Acceptance Criteria

1. THE Career_Agent SHALL define a Job_Portal interface that abstracts the following capabilities: job ingestion, session management, application form detection, application form filling, and application submission.
2. THE Career_Agent SHALL implement a LinkedIn Portal_Adapter as the first implementation of the Job_Portal interface.
3. THE Career_Agent SHALL allow registration of new Portal_Adapter implementations through Spring dependency injection without requiring changes to the core workflow engine, matching logic, or application preparation logic.
4. WHEN a new Portal_Adapter is registered, THE Career_Agent SHALL automatically include that portal's jobs in the discovery workflow and make the portal's auto-apply capability available if the adapter supports application submission.
5. THE Career_Agent SHALL support Portal_Adapters that provide only job ingestion (read-only) and Portal_Adapters that provide both job ingestion and application submission (read-write).
6. THE Career_Agent SHALL store the portal identifier (e.g., LINKEDIN, INDEED, GREENHOUSE) on each Job record to track which portal the job originated from and which portal should be used for auto-apply.
7. THE Career_Agent SHALL route auto-apply requests to the appropriate Portal_Adapter based on the job's portal identifier.

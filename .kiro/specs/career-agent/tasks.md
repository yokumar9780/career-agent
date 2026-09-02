# Implementation Plan: Career Agent

## Overview

The Career Agent is implemented as 19 vertical feature slices, each producing a working, end-to-end testable increment. Every task wires frontend (Next.js 16 / TypeScript), backend (Spring Boot 4.1 / Java 25), and database (PostgreSQL 17) together so the user can test from the browser after each task completes. The Browser Automation Service (Express / TypeScript / Playwright) is introduced in its own slice once the core application loop is solid.

Tasks are ordered so each builds on the previous with no forward dependencies. Property-based tests reference the design document's Correctness Properties section. Unit and integration tests are placed as optional sub-tasks within each feature slice.

## Tasks

- [x] 1. Project Setup & Health Check
  - [x] 1.1 Scaffold Spring Boot 4.1 backend project (`career-agent-service`)
    - Create Maven project with `pom.xml` including all dependencies from the design (Spring Boot 4.1, Spring AI 2.0.1, PostgreSQL, Flyway, jjwt, Lombok, springdoc, pdfbox, poi-ooxml, jqwik)
    - Create `CareerAgentApplication.java` main class
    - Create `application.yml` with externalized config (DB, CORS, schedule defaults) using env vars
    - Create Actuator health endpoint at `/actuator/health`
    - Create `CorsConfig` allowing configurable origins
    - _Requirements: 17.1, 17.4, 17.5, 18.1, 18.2, 18.4_

  - [x] 1.2 Scaffold Next.js 16 frontend project (`career-agent-ui`)
    - Initialize Next.js 16 project with TypeScript, Tailwind CSS 4, App Router
    - Install all frontend dependencies from the design (`axios`, `@tanstack/react-query`, `zustand`, `date-fns`, `zod`, `lucide-react`, `shadcn/ui`)
    - Create root layout and basic app shell
    - Create `/api/health` health route
    - Create Axios instance with base URL config pointing to backend
    - _Requirements: 17.2_

  - [x] 1.3 Set up Docker Compose and PostgreSQL
    - Create `docker-compose.yml` with `postgres:17` service, named volume `pgdata`, health check (`pg_isready`)
    - Add `career-agent-service` and `career-agent-ui` service stubs with correct `depends_on`, port mappings (8080, 3000, 5432)
    - Create `.env.example` with all required environment variables documented
    - Create initial Flyway migration `V1__init.sql` (empty schema placeholder)
    - _Requirements: 17.3, 17.6, 17.7, 16.1, 16.5_

  - [x] 1.4 Wire frontend to backend health check
    - Create a simple landing page in Next.js that fetches `/actuator/health` from the backend and displays connection status
    - Verify CORS allows frontend origin
    - _Requirements: 18.4_

  - **How to test E2E**: Run `docker-compose up` (or run services locally). Open `http://localhost:3000` — see landing page showing backend health status as "UP". Hit `http://localhost:8080/actuator/health` directly — see JSON health response.

- [ ] 2. User Registration & Login
  - [~] 2.1 Implement candidate registration and login backend
    - Create Flyway migration `V2__candidate_profile.sql` with `candidate_profile` table (id, email, password_hash, name, phone, summary, application_mode, pre_submit_review, match_score_threshold, timezone, schedule_cron, active, created_at, updated_at)
    - Create `CandidateProfile` JPA entity and `CandidateProfileRepository`
    - Create `SecurityConfig` with JWT stateless auth, bcrypt password encoder (cost 10), public endpoints for `/api/v1/auth/**`
    - Create `JwtTokenProvider` (generate, validate, extract candidateId) with configurable expiration (default 24h)
    - Create `JwtAuthFilter` that extracts token from Authorization header, validates, sets SecurityContext
    - Create `POST /api/v1/auth/register` — validate email uniqueness, password complexity (8-128 chars, uppercase, lowercase, digit), hash password, create profile
    - Create `POST /api/v1/auth/login` — validate credentials, return `{ accessToken, expiresIn }`
    - Implement consistent error response format (`timestamp`, `status`, `error`, `message`, `path`) via `@ControllerAdvice`
    - _Requirements: 12.1, 12.2, 12.3, 12.5, 12.6, 12.7, 13.1, 13.2, 13.3, 13.6, 18.3, 18.6_

  - [~] 2.2 Implement registration and login frontend
    - Create `(auth)/register/page.tsx` with registration form (email, password, name)
    - Create `(auth)/login/page.tsx` with login form (email, password)
    - Create Zustand auth store to hold JWT token in memory
    - Configure Axios interceptor to attach `Authorization: Bearer <token>` and handle 401 redirect to login
    - After successful login, redirect to dashboard shell
    - Create `(dashboard)/layout.tsx` authenticated layout with sidebar placeholder
    - _Requirements: 12.1, 12.2, 12.4_

  - [ ]* 2.3 Write property tests for password validation and error response format
    - **Property 18: Password Validation Rules** — generate random strings, verify accept/reject matches complexity rules
    - **Validates: Requirements 12.6**
    - **Property 17: Error Response Format Consistency** — trigger various 4xx/5xx errors, verify all responses contain required fields
    - **Validates: Requirements 13.3, 18.6**

  - [ ]* 2.4 Write unit tests for JWT and auth flow
    - Test expired token rejection (401), malformed token (401), missing token (401)
    - Test bcrypt hashing and verification
    - Test duplicate email registration (409)
    - _Requirements: 12.3, 12.6, 12.7_

  - **How to test E2E**: Open `http://localhost:3000/register` — fill in form, submit. See success, redirected to login. Login with credentials. Verify token stored (check Axios headers in DevTools). Access dashboard layout. Try registering duplicate email — see error. Try weak password — see validation error.

- [~] 3. Checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 4. Candidate Profile CRUD
  - [~] 4.1 Implement profile and preferences backend
    - Create Flyway migration `V3__candidate_preference_and_document.sql` with `candidate_preference` table (target_job_titles, preferred_locations, remote_preference, min_salary, preferred_industries, target_companies, seniority_level, must_have_requirements, exclusions) and `candidate_document` table (filename, content_type, file_size, storage_path, extracted_text, primary_cv, uploaded_at)
    - Create `CandidatePreference` and `CandidateDocument` JPA entities and repositories
    - Create `ProfileController` with CRUD endpoints: `GET/PUT /api/v1/profiles/me`, `GET/PUT /api/v1/profiles/me/preferences`
    - Implement `ValidationService` — salary range (0.01–999,999,999.99), seniority enum, location string length (≤200 chars, ≤20 entries), profile name (≤200), free-text (≤5000)
    - Implement HTML/script stripping on all text inputs (sanitization)
    - Implement profile activation validation (≥1 target job title + ≥1 preferred location)
    - Implement resource-level authorization (candidateId from JWT must match resource owner, else 403)
    - _Requirements: 1.2, 1.4, 1.5, 2.1, 2.2, 2.5, 2.6, 13.4, 13.5_

  - [~] 4.2 Implement CV upload and document management backend
    - Create `POST /api/v1/profiles/me/documents` — accept PDF/DOC/DOCX, max 5MB, max 5 docs per profile
    - Create `GET /api/v1/profiles/me/documents` and `DELETE /api/v1/profiles/me/documents/{id}`
    - Implement `DocumentStorageService` for local filesystem (configurable path)
    - Integrate PDF text extraction via Apache PDFBox, DOCX via Apache POI
    - _Requirements: 1.1, 1.7, 2.3_

  - [~] 4.3 Implement profile creation and management frontend
    - Create `(dashboard)/profile/page.tsx` with profile view/edit form
    - Show extracted CV data for review after upload, allow editing before save
    - Preference editing form with all fields (job titles, locations, remote pref, salary, industries, companies, seniority, must-haves, exclusions)
    - CV upload component with drag-and-drop, file type/size validation
    - Document list showing uploaded CVs with delete action
    - Display validation errors inline on form fields
    - _Requirements: 1.3, 1.6, 2.1, 2.3, 2.5_

  - [~] 4.4 Implement profile deletion with cascade
    - `DELETE /api/v1/profiles/me` — delete profile, preferences, documents, stored files
    - _Requirements: 2.4_

  - [ ]* 4.5 Write property tests for profile validation
    - **Property 1: Profile Validation Completeness** — vary job titles list and locations list, verify activation rules
    - **Validates: Requirements 1.4**
    - **Property 2: CV Upload Constraints Enforcement** — vary file format, size, existing doc count, verify accept/reject
    - **Validates: Requirements 2.3**
    - **Property 3: Profile Preference Validation Round-Trip** — save and reload preferences with varied valid/invalid inputs
    - **Validates: Requirements 2.2, 2.5**
    - **Property 4: Profile Deletion Cascades Completely** — create profile with associated data, delete, verify no orphans
    - **Validates: Requirements 2.4**
    - **Property 5: Authorization Isolation** — two candidates, verify cross-access returns 403
    - **Validates: Requirements 2.6, 12.4, 12.5**
    - **Property 16: Input Sanitization Idempotence** — apply HTML stripping twice, verify same result
    - **Validates: Requirements 13.4**
    - **Property 22: Text Field Length Enforcement** — vary string lengths per field, verify reject/accept at boundaries
    - **Validates: Requirements 13.5**

  - [ ]* 4.6 Write unit and integration tests for profile CRUD
    - Test CV extraction from PDF and DOCX
    - Test preference validation edge cases (boundary salary values, max locations)
    - Integration test: full create → read → update → delete lifecycle with Testcontainers
    - _Requirements: 1.1, 2.1, 2.2, 2.3, 2.4_

  - **How to test E2E**: Login, navigate to Profile page. Upload a PDF CV — see extracted data appear for review. Edit fields, save preferences. Try invalid salary — see validation error. Upload 6th document — see rejection. Navigate away and back — data persists. Delete profile — verify data gone.

- [~] 5. Checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 6. Job Portal Abstraction & LinkedIn Email Ingestion
  - [~] 6.1 Implement Job Portal abstraction layer and job storage
    - Create Flyway migration `V4__job_and_workflow.sql` with `job` table (all fields from ER diagram including portal_identifier, source_types, source_urls), `job_status_history` table, and `workflow_execution` table
    - Create `Job`, `JobStatusHistory`, `WorkflowExecution` JPA entities and repositories
    - Create `JobPortal` interface (getPortalIdentifier, getSourceAdapters, supportsAutoApply, etc.)
    - Create `JobSourceAdapter` interface (getSourceType, ingestJobs)
    - Create `JobStatus` enum with all statuses and valid transitions map
    - Implement status transition validation service — reject invalid transitions with error listing valid next statuses
    - _Requirements: 24.1, 24.3, 24.5, 10.1, 10.2, 10.6, 5.5_

  - [~] 6.2 Implement LinkedIn Email Ingestion Adapter
    - Create `LinkedInPortalAdapter` implementing `JobPortal`
    - Create `LinkedInEmailIngestionAdapter` implementing `JobSourceAdapter`
    - Create `EmailListener` — IMAP connection via Spring Boot Mail, configurable host/port/user/credentials/folder
    - Create `EmailParser` — parse LinkedIn Job Alert HTML, extract job title, company, location, URL
    - Handle missing URL (skip posting, log warning), unrecognized format (log, mark processed)
    - Deduplicate by URL on ingestion (skip if URL exists)
    - Process up to 200 unprocessed emails per run, mark as processed
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 24.2_

  - [~] 6.3 Implement Job Ingestion Service and basic job list API
    - Create `JobIngestionService` that iterates all registered `JobSourceAdapter` beans
    - Create `JobController` with `GET /api/v1/jobs` (paginated, filterable) and `GET /api/v1/jobs/{id}`
    - _Requirements: 18.1, 18.5_

  - [~] 6.4 Implement basic job list frontend
    - Create `(dashboard)/jobs/page.tsx` showing ingested jobs in a table (title, company, location, status, source)
    - Create `(dashboard)/jobs/[id]/page.tsx` job detail view
    - Add pagination component
    - _Requirements: 8.7_

  - [ ]* 6.5 Write property tests for job status transitions and pagination
    - **Property 13: Job Status Transition Validity** — test all (from, to) pairs against valid transitions table
    - **Validates: Requirements 10.1, 10.2, 10.6**
    - **Property 15: Pagination Metadata Consistency** — vary total elements and page sizes, verify math
    - **Validates: Requirements 18.5, 8.7**

  - [ ]* 6.6 Write unit tests for email parsing and ingestion
    - Test specific LinkedIn email HTML structures, malformed emails, zero-posting emails
    - Test URL deduplication on ingestion
    - Test IMAP connection failure handling
    - _Requirements: 3.2, 3.3, 3.5, 3.6, 3.7_

  - **How to test E2E**: Configure email inbox in `.env`. Login, navigate to Jobs page. Trigger ingestion (via API or wait for schedule). See raw jobs appear in the list with status NEW. Click a job to see detail. Verify duplicates by URL are not created. Check pagination works.

- [ ] 7. Job Normalization & Deduplication
  - [~] 7.1 Implement Job Analysis Agent with Spring AI
    - Create `JobAnalysisAgentConfig` with ChatClient bean and system prompt (`prompts/job-analysis-agent.md`)
    - Create `JobAnalysisAgent` service — takes raw job data, calls LLM, returns normalized Job
    - Implement `LlmRateLimiter` — token-bucket (30/min default), queue (max 500), configurable
    - Implement retry with exponential backoff (2s, 4s, 8s), max 3 retries, 120s timeout
    - Create `ChatClientConfig` for Spring AI ChatClient builder
    - Normalize all required fields: title, company, location, remoteType, salaryRange, description, requirements, skills, url, source, postedDate
    - Use UNSPECIFIED for undetermined fields, ISO 8601 dates (fallback to ingestion date)
    - Extract up to 50 skills, each ≤100 chars
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.6, 14.1, 14.2, 14.3, 14.4, 14.5, 14.6, 14.7_

  - [~] 7.2 Implement Deduplication Service
    - Create `DeduplicationService` — match on company (case-insensitive) + title (case-insensitive) + location (case-insensitive)
    - Merge duplicate: retain all distinct source URLs and source identifiers, use newer data for differing fields
    - Log each detected duplicate with matched job ID
    - Create new record if no match
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

  - [~] 7.3 Wire normalization and dedup into ingestion pipeline
    - Update `JobIngestionService` to run: ingest → normalize → deduplicate for each raw job
    - Update job status: NEW → ANALYZED after normalization
    - Update frontend job list to show normalized fields (remote type badge, skills tags, salary range)
    - _Requirements: 5.1, 5.7_

  - [ ]* 7.4 Write property tests for normalization and deduplication
    - **Property 8: Job Normalization Schema Completeness** — vary raw job data, verify all required fields present
    - **Validates: Requirements 5.1, 5.4**
    - **Property 9: Remote Type Classification** — verify remoteType is exactly one of REMOTE, HYBRID, ON_SITE, UNSPECIFIED
    - **Validates: Requirements 5.2**
    - **Property 6: Email Deduplication by URL** — sequences of postings with duplicate URLs, verify distinct record count
    - **Validates: Requirements 3.5**
    - **Property 10: Deduplication Merge Preserves Sources** — matching job pairs, verify merged record has all sources
    - **Validates: Requirements 6.1, 6.2, 6.3**
    - **Property 19: LLM Retry Exhaustion Marks Failure** — simulate transient failures, verify retry behavior and failure marking
    - **Validates: Requirements 14.2, 14.3**

  - [ ]* 7.5 Write unit tests for normalization and LLM rate limiter
    - Test rate limiter token bucket behavior
    - Test queue-full rejection
    - Test LLM timeout treated as transient failure
    - _Requirements: 14.4, 14.5, 14.6, 14.7_

  - **How to test E2E**: Ingest jobs from email. See jobs transition from NEW to ANALYZED on the jobs page. Verify normalized fields display (remote type, skills, salary). Ingest same jobs again — verify no duplicates, sources merged. Check dedup logged.

- [ ] 8. AI Job Matching
  - [~] 8.1 Implement Matching Agent with Spring AI
    - Create `MatchingAgentConfig` with ChatClient bean and system prompt (`prompts/matching-agent.md`)
    - Create `MatchingAgent` service — takes Job + CandidateProfile, returns MatchResult
    - Create Flyway migration `V5__job_match.sql` with `job_match` table
    - Create `JobMatch` entity and `JobMatchRepository`
    - Produce MatchResult: overall score (0-100), recommendation (APPLY/SKIP), dimension scores (skills, experience, location, salary, seniority each 0-100), strengths (1-10), gaps (0-10), risks (0-10), summary
    - Route based on threshold: score ≥ threshold → SHORTLISTED, else → SKIPPED
    - Handle LLM failure: log, set status ANALYZED (pending retry), continue
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_

  - [~] 8.2 Wire matching into workflow pipeline and expose API
    - Update pipeline: ingest → normalize → deduplicate → match
    - Add match threshold to candidate settings (`match_score_threshold`, default 60, configurable 1-100)
    - `GET /api/v1/jobs/{id}/match` — return MatchResult for a job
    - _Requirements: 7.7_

  - [~] 8.3 Display match results on frontend
    - Update job list to show match score column, recommendation badge
    - Update job detail page to show full MatchResult: dimension scores (radar/bar chart), strengths, gaps, risks, summary
    - _Requirements: 8.1, 8.3_

  - [ ]* 8.4 Write property tests for matching
    - **Property 11: Match Score Determines Shortlist Status** — vary scores and thresholds, verify SHORTLISTED/SKIPPED
    - **Validates: Requirements 7.4, 7.5, 7.7**
    - **Property 12: MatchResult Structural Validity** — verify score ranges, list lengths, recommendation values
    - **Validates: Requirements 7.2, 7.3**

  - [ ]* 8.5 Write unit tests for matching edge cases
    - Test threshold boundary (score exactly equals threshold)
    - Test LLM failure → status remains ANALYZED
    - Test MatchResult with minimum/maximum list sizes
    - _Requirements: 7.4, 7.5, 7.6_

  - **How to test E2E**: Login, ensure profile is active with preferences. Trigger workflow. See jobs scored on the jobs page. Click a job — see full match breakdown with dimension scores, strengths, gaps, risks. Change threshold in settings — re-match jobs, verify different shortlist results.

- [~] 9. Checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 10. Shortlist Dashboard
  - [~] 10.1 Implement shortlist API endpoints
    - Create `DashboardController` with `GET /api/v1/dashboard/summary` (total ingested, shortlisted, skipped, applications in progress)
    - Enhance `GET /api/v1/jobs` with filters: min score, location, company, title keyword, status, date range
    - Add `POST /api/v1/jobs/{id}/skip` and `POST /api/v1/jobs/{id}/restore` endpoints
    - Record status changes in `job_status_history`
    - _Requirements: 8.2, 8.4, 8.5, 8.6, 8.8, 10.5_

  - [~] 10.2 Build full Shortlist Dashboard UI
    - Create `(dashboard)/jobs/page.tsx` as the primary shortlist dashboard
    - Display shortlisted jobs ranked by score desc: title, company, location, remote type, score, summary
    - Filter bar: min score slider, location dropdown, company text, title keyword, status select, date range picker
    - Summary counts bar: total ingested, shortlisted, skipped, in progress
    - Action buttons per job: "Prepare Application", "Skip", "View Details"
    - Pagination with configurable page size (1-100, default 20)
    - "Skipped" tab with "Restore" action
    - Empty state message when no jobs match filters
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8, 8.9_

  - [ ]* 10.3 Write property tests for status history
    - **Property 14: Status History Completeness** — perform N transitions, verify exactly N history records with correct sequence
    - **Validates: Requirements 10.5**

  - [ ]* 10.4 Write unit tests for dashboard filtering and pagination
    - Test filter combinations with empty results
    - Test pagination edge cases (last page, single item)
    - Test skip/restore status transitions
    - _Requirements: 8.2, 8.7, 8.8, 8.9_

  - **How to test E2E**: Login, navigate to dashboard. See shortlisted jobs ranked by score. Apply filters — verify list updates. Click "Skip" — job moves to skipped tab. Go to skipped tab, click "Restore" — job returns. Paginate through results. View summary counts. Click "View Details" — see full job + match result.

- [ ] 11. Application Package Preparation
  - [~] 11.1 Implement Application Agent and package generation backend
    - Create `ApplicationAgentConfig` with ChatClient bean and system prompt (`prompts/application-agent.md`)
    - Create `ApplicationAgent` service — generate CV recommendation, cover letter (highlight strengths, address gaps), screening answers (up to 5)
    - Create Flyway migration `V6__application.sql` with `application` and `application_document` tables
    - Create `Application`, `ApplicationDocument` entities and repositories
    - Create `ApplicationController` with:
      - `POST /api/v1/applications` (jobId) — validate job is SHORTLISTED with MatchResult, generate package, return for review
      - `GET /api/v1/applications/{id}` — get application with documents
      - `PUT /api/v1/applications/{id}/documents` — save edits to cover letter, CV rec, screening answers
      - `POST /api/v1/applications/{id}/approve` — approve package, set status READY_TO_APPLY
    - Enforce 90s timeout on package generation, allow retry on failure
    - Store all documents as ApplicationDocument records with versioning
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8_

  - [~] 11.2 Build application preparation UI
    - Add "Prepare Application" button on shortlist dashboard (already wired from task 10)
    - Create `(dashboard)/applications/[id]/page.tsx` — show generated materials: CV recommendation, cover letter (editable), screening answers (editable)
    - Edit/save functionality for cover letter and screening answers
    - "Approve" button to finalize
    - Error state if generation fails with retry button
    - _Requirements: 9.3, 9.4, 9.5, 9.6_

  - [ ]* 11.3 Write unit tests for application package generation
    - Test package generation from specific MatchResult scenarios
    - Test rejection when job not SHORTLISTED or no MatchResult
    - Test 90s timeout handling
    - _Requirements: 9.1, 9.6, 9.8_

  - **How to test E2E**: From shortlist dashboard, click "Prepare Application" on a job. Wait for AI generation (≤90s). See CV recommendation, cover letter, screening answers. Edit the cover letter. Save. Approve the package. Verify job status changes to READY_TO_APPLY. Try preparing for a non-shortlisted job — see error.

- [ ] 12. Application Tracking
  - [~] 12.1 Implement application tracking backend
    - Enhance `ApplicationController` with:
      - `POST /api/v1/applications/{id}/applied` — record applied date, CV version, cover letter version, set status APPLIED
      - `PUT /api/v1/applications/{id}/status` — update status (INTERVIEW, OFFER, CLOSED, REJECTED) with transition validation
    - Create `ApplicationTrackingService` — enforce valid transitions, record every change with timestamp in `job_status_history`
    - `GET /api/v1/applications` — paginated list with company, title, applied date, score, status, days since last change, sorted by most recent change
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_

  - [~] 12.2 Build application tracking UI
    - Create `(dashboard)/applications/page.tsx` — tracking table: company, title, applied date, score, status, days since last change
    - Status update dropdown per application (valid next statuses only)
    - Click row to view application detail with full package
    - _Requirements: 10.4_

  - [ ]* 12.3 Write property tests for application mode inheritance
    - **Property 21: Application Mode Inheritance** — vary global mode and per-job overrides, verify effective mode
    - **Validates: Requirements 19.4**

  - [ ]* 12.4 Write unit tests for status transitions
    - Test invalid transition rejection with helpful error message
    - Test "Mark as Applied" records date and versions
    - _Requirements: 10.3, 10.6_

  - **How to test E2E**: Navigate to Applications page. See tracked applications. Click "Mark as Applied" on a READY_TO_APPLY job — enter applied date. Update status to INTERVIEW, then OFFER. Try invalid transition — see error with valid options. Verify history timestamps.

- [ ] 13. Scheduled Workflow Execution
  - [~] 13.1 Implement workflow scheduler and execution tracking
    - Create `WorkflowEngine` — orchestrate full pipeline: ingest → normalize → deduplicate → match → shortlist
    - Create `WorkflowScheduler` with `@Scheduled(cron)` — configurable schedule, default daily 08:00 UTC, minimum 1h interval
    - Create `WorkflowExecutionService` — create/update WorkflowExecution records (RUNNING, COMPLETED, FAILED)
    - Track counts: jobs_ingested, duplicates_detected, jobs_matched, jobs_shortlisted
    - Handle stage failures: log, set FAILED with error_description, skip stage, continue
    - Create `POST /api/v1/workflows/trigger` — manual trigger, reject if already running
    - Create `GET /api/v1/workflows` — paginated execution history
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7_

  - [~] 13.2 Build workflow management UI
    - Create `(dashboard)/workflows/page.tsx` — execution history table: trigger type, status, timing, counts
    - "Run Now" button to trigger manual workflow
    - Show "already running" message if in progress
    - _Requirements: 11.5, 11.6_

  - [ ]* 13.3 Write property tests for workflow mutual exclusion
    - **Property 23: Workflow Execution Mutual Exclusion** — simulate concurrent triggers, verify at most one RUNNING
    - **Validates: Requirements 11.6**

  - [ ]* 13.4 Write integration tests for workflow pipeline
    - Test full pipeline with mock LLM and email — ingest, normalize, dedup, match
    - Test manual trigger rejection when already running
    - _Requirements: 11.1, 11.4, 11.6_

  - **How to test E2E**: Login, navigate to Workflows page. Click "Run Now" — see execution appear as RUNNING, then COMPLETED with counts. Click "Run Now" while running — see rejection message. Verify new jobs appear on shortlist after workflow completes.

- [~] 14. Checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 15. Company Career Page Ingestion
  - [~] 15.1 Implement career page scraper and URL management backend
    - Create Flyway migration `V7__company_career_page.sql` with `company_career_page` table
    - Create `CompanyCareerPage` entity and repository
    - Create `LinkedInCareerPageAdapter` implementing `JobSourceAdapter`
    - Implement career page fetching with 30s timeout, HTTP error handling
    - Extract job title, company, location, URL from page HTML
    - Deduplicate by URL on ingestion
    - Max 50 URLs per candidate
    - URL validation: ≤2048 chars, valid syntax, HTTPS only
    - Create `POST/GET/DELETE /api/v1/career-pages` endpoints on `ProfileController` or new controller
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7_

  - [~] 15.2 Build career page management UI
    - Add career page URL management to Settings or Profile page
    - Add/edit/remove URLs with validation feedback
    - Show list of configured career pages with last scraped date
    - _Requirements: 4.4_

  - [ ]* 15.3 Write property tests for career page URL validation
    - **Property 7: Career Page URL Validation** — vary URL strings (length, scheme, syntax), verify accept/reject
    - **Validates: Requirements 4.6, 4.7**

  - [ ]* 15.4 Write unit tests for career page scraping
    - Test HTML parsing for specific patterns
    - Test timeout and HTTP error handling
    - Test URL deduplication
    - _Requirements: 4.2, 4.3, 4.5_

  - **How to test E2E**: Go to Settings/Profile. Add a career page URL (try HTTP — rejected, try valid HTTPS — accepted). Trigger workflow. See jobs from career page appear in the job list. Add invalid URL — see validation error. Try adding 51st URL — rejected.

- [ ] 16. Application Mode Configuration
  - [~] 16.1 Implement application mode settings backend
    - Add `PUT /api/v1/profiles/me/settings` — update application_mode (MANUAL/AUTO_APPLY), pre_submit_review (ENABLED/DISABLED), match_score_threshold
    - Add `application_mode_override` field on Application entity for per-job override
    - Implement mode inheritance: per-job override takes precedence over global setting
    - Ensure mode changes only affect future submissions, not existing READY_TO_APPLY
    - _Requirements: 19.1, 19.2, 19.4, 19.5, 22.1_

  - [~] 16.2 Build application mode settings UI
    - Create `(dashboard)/settings/page.tsx` — app settings form
    - Application mode toggle (MANUAL / AUTO_APPLY)
    - Pre-submit review toggle (ENABLED / DISABLED)
    - Match score threshold slider (1-100)
    - Per-job mode override on job detail page
    - _Requirements: 19.1, 22.1_

  - [ ]* 16.3 Write unit tests for mode inheritance and settings
    - Test global mode inheritance when no override
    - Test per-job override takes precedence
    - Test mode change doesn't affect existing submissions
    - _Requirements: 19.4, 19.5_

  - **How to test E2E**: Go to Settings. Toggle application mode to AUTO_APPLY. Set pre-submit review to ENABLED. Save. On a job detail page, override to MANUAL for that job. Verify behavior follows override. Change global mode back — verify existing submissions unaffected.

- [ ] 17. Browser Automation Service Setup
  - [~] 17.1 Scaffold Browser Automation Service (`browser-automation-service`)
    - Initialize Express 5 + TypeScript project with all dependencies from design (playwright, winston, helmet, express-rate-limit, zod)
    - Create `PortalSubmissionHandler` interface and `PortalHandlerRegistry`
    - Create health endpoint at `GET /health` — report service availability, browser status, submission counts
    - Create REST endpoints: `POST /auth/start`, `POST /auth/validate`, `POST /submit`, `POST /submit/:id/confirm`, `POST /submit/:id/cancel`
    - Implement rate limiting: 10 submissions/hour, 30/day per candidate
    - Implement randomized delay 30-90s between consecutive submissions
    - Add service to Docker Compose with health check
    - _Requirements: 20.1, 20.2, 20.8, 20.9, 20.10, 20.11_

  - [~] 17.2 Implement backend client for Browser Automation Service
    - Create `BrowserAutomationClient` — REST client for calling browser automation endpoints
    - Create `SubmissionResultHandler` — process submission results, update job status
    - Wire into `ApplicationController` for auto-apply flow
    - _Requirements: 20.1_

  - [ ]* 17.3 Write unit tests for browser automation service
    - Test rate limit enforcement (10/hour, 30/day)
    - Test handler registry routing
    - Test health endpoint response format
    - _Requirements: 20.8, 20.10, 20.11_

  - **How to test E2E**: Start browser automation service (Docker or local). Hit `http://localhost:4000/health` — see status, browser state, submission counts. From backend, verify client can connect to service.

- [ ] 18. Portal Session Management
  - [~] 18.1 Implement portal session storage and encryption backend
    - Create Flyway migration `V8__portal_session.sql` with `portal_session` table (candidate_id, portal_identifier, encrypted_session bytea, created_at, expires_at)
    - Create `PortalSession` entity and repository
    - Create `SessionEncryptionService` — AES-256-GCM encryption, key from `PORTAL_ENCRYPTION_KEY` env var, random 12-byte IV per encryption
    - Create `PortalSessionManager` — store/retrieve/delete sessions keyed by (candidateId, portalId)
    - Session TTL: 30 days, auto-cleanup via `SessionCleanupScheduler`
    - Deletion within 5s on revoke or account delete
    - Create endpoints: `GET /api/v1/portals/{portalId}/auth/start`, `DELETE /api/v1/portals/{portalId}/session`
    - _Requirements: 21.1, 21.4, 21.5, 21.6, 21.7_

  - [~] 18.2 Implement LinkedIn login flow in Browser Automation Service
    - Implement `POST /auth/start` — launch Playwright (headed), open LinkedIn login, wait for candidate login, extract cookies, return to backend
    - Implement `POST /auth/validate` — validate session cookies against LinkedIn
    - Implement `LinkedInSessionHandler`
    - _Requirements: 21.1, 21.2, 21.3_

  - [~] 18.3 Build portal session management UI
    - Add LinkedIn connection section to Settings page
    - "Connect LinkedIn" button → opens auth flow
    - Show connection status (connected/disconnected, expiry date)
    - "Disconnect" button to revoke session
    - _Requirements: 21.1, 21.6_

  - [ ]* 18.4 Write property tests for portal session isolation
    - **Property 26: Portal Session Isolation** — multiple candidates with sessions for multiple portals, verify isolation on revoke and delete
    - **Validates: Requirements 21.1, 21.6, 21.7**

  - [ ]* 18.5 Write unit tests for session encryption and lifecycle
    - Test encrypt/decrypt round-trip
    - Test session expiry cleanup
    - Test deletion within 5s
    - _Requirements: 21.5, 21.6_

  - **How to test E2E**: Go to Settings. Click "Connect LinkedIn". LinkedIn login page opens in new browser. Enter credentials. See "Connected" status appear with expiry date. Click "Disconnect" — status returns to disconnected.

- [ ] 19. LinkedIn Auto-Apply (Easy Apply)
  - [~] 19.1 Implement LinkedIn Easy Apply submission handler
    - Create `LinkedInEasyApplyHandler` implementing `PortalSubmissionHandler`
    - Implement `detectApplicationType(page)` — detect Easy Apply vs external redirect
    - Implement `fillForm(page, applicationData)` — handle single-page and multi-step (up to 10 steps), file upload (PDF/DOCX), text inputs, dropdowns, checkboxes/radios
    - Implement `submit(page, filledForm)` — click submit, detect confirmation within 30s
    - Handle unmatched screening questions — pause, report back
    - Handle EXTERNAL_REDIRECT — fallback to MANUAL
    - _Requirements: 20.3, 20.4, 20.5, 20.6, 20.7_

  - [~] 19.2 Wire auto-apply into application approval flow
    - When Application_Mode is AUTO_APPLY and candidate approves package:
      - Load portal session, validate
      - Route to appropriate PortalSubmissionHandler via registry
      - Submit application within 60s of approval
      - On success: set APPLIED, record timestamp, store confirmation screenshot
      - On failure: set SUBMISSION_FAILED, store failure screenshot, notify candidate within 60s
    - _Requirements: 19.3, 19.6, 19.7, 23.1, 23.2, 23.3_

  - [~] 19.3 Update frontend for auto-apply status
    - Show auto-apply status on application detail (submitting, submitted, failed)
    - Show notification on successful submission
    - Show failure notification with retry option
    - _Requirements: 19.6, 19.7, 23.3_

  - [ ]* 19.4 Write property tests for portal adapter routing
    - **Property 24: Portal Adapter Registration** — mock adapters with varying capabilities, verify discovery and auto-apply availability
    - **Validates: Requirements 24.3, 24.4, 24.5**
    - **Property 25: Portal Auto-Apply Routing** — jobs with various portal identifiers, verify correct routing or rejection
    - **Validates: Requirements 24.6, 24.7**

  - [ ]* 19.5 Write unit tests for Easy Apply handler
    - Test form detection (Easy Apply vs external redirect)
    - Test multi-step form filling
    - Test unmatched screening question handling
    - _Requirements: 20.3, 20.4, 20.5, 20.6, 20.7_

  - **How to test E2E**: Set Application_Mode to AUTO_APPLY. Connect LinkedIn session. Approve an application package. See submission status update — either APPLIED with confirmation or SUBMISSION_FAILED with screenshot. For external redirect jobs, see fallback to MANUAL notification.

- [~] 20. Checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 21. Pre-Submit Review Gate
  - [~] 21.1 Implement pre-submit review backend and browser automation
    - When Pre_Submit_Review is ENABLED: after form filling, pause before submit
    - Browser Automation Service takes screenshot of filled form
    - Send paused state to backend with screenshot, form summary (job title, company, CV, cover letter, answers)
    - `POST /api/v1/submissions/{id}/confirm` — resume and submit within 30s
    - `POST /api/v1/submissions/{id}/cancel` — cancel, return to READY_TO_APPLY
    - 24-hour timeout: auto-cancel, close browser session, return to READY_TO_APPLY
    - Handle stale browser session during pending review — cancel, notify, allow retry
    - _Requirements: 22.2, 22.3, 22.4, 22.5, 22.6, 22.7, 22.9_

  - [~] 21.2 Build pre-submit review UI
    - Show pending review notification badge on dashboard
    - Review page: job title, company, CV being uploaded, cover letter, screening answers, form screenshot
    - "Approve & Submit" and "Reject & Edit" buttons
    - _Requirements: 22.3, 22.8_

  - [ ]* 21.3 Write unit tests for pre-submit review flow
    - Test 24-hour timeout cancellation
    - Test stale session handling
    - Test approve/reject flows
    - _Requirements: 22.4, 22.5, 22.6, 22.9_

  - **How to test E2E**: Enable Pre_Submit_Review in settings. Approve an application in AUTO_APPLY mode. See notification badge appear. Open review — see summary with screenshot. Click "Approve & Submit" — application submits. Test "Reject & Edit" — returns to READY_TO_APPLY.

- [ ] 22. Submission Result & Retry Handling
  - [~] 22.1 Implement submission result handling and retry logic
    - Create Flyway migration `V9__auto_apply_attempt.sql` with `auto_apply_attempt` table
    - Create `AutoApplyAttempt` entity and repository
    - Record every attempt: timestamp, job_id, portal_identifier, result (SUCCESS/FAILED), failure_reason, screenshot_doc_id
    - Implement retry: allow up to 3 retries from SUBMISSION_FAILED → READY_TO_APPLY
    - After 3 failures: block further auto-apply, notify to switch to MANUAL
    - Handle CAPTCHA: pause, notify candidate, 5-min timeout → SUBMISSION_FAILED with CAPTCHA_TIMEOUT reason
    - _Requirements: 23.1, 23.2, 23.3, 23.4, 23.5, 23.6, 23.7, 23.8, 23.9_

  - [~] 22.2 Update frontend for retry and failure handling
    - Show retry button on SUBMISSION_FAILED applications (up to 3 retries)
    - Show retry count
    - After 3 failures, show "Switch to Manual" prompt
    - CAPTCHA notification with manual solve instruction
    - _Requirements: 23.4, 23.9_

  - [ ]* 22.3 Write property tests for auto-apply retry limit
    - **Property 20: Auto-Apply Retry Limit Enforcement** — vary retry counts 0-5, verify allow/block behavior
    - **Validates: Requirements 23.4, 23.9**

  - [ ]* 22.4 Write unit tests for submission result handling
    - Test confirmation detection within 30s
    - Test CAPTCHA timeout at 5 minutes
    - Test screenshot storage on success and failure
    - _Requirements: 23.1, 23.3, 23.5, 23.6_

  - **How to test E2E**: Trigger auto-apply on a job. If it fails, see SUBMISSION_FAILED status with screenshot. Click "Retry" — see attempt count increment. After 3 failures, retry button disappears, "Switch to Manual" shown. Verify attempt history recorded.

- [ ] 23. Observability & Production Hardening
  - [~] 23.1 Implement structured logging and correlation IDs
    - Create `CorrelationIdFilter` — generate UUID v4 for each request, propagate via MDC to all log entries
    - Accept `X-Correlation-ID` header or generate new
    - Configure structured JSON log output: timestamp (ISO 8601), level, logger, message, correlationId
    - Log every LLM call: agent name, input/output token counts, latency, success/failure
    - Log workflow summary at INFO: execution ID, duration, all counts
    - Log ERROR with correlationId, exception type, message for unhandled exceptions
    - _Requirements: 15.1, 15.2, 15.6, 15.7, 14.8_

  - [~] 23.2 Implement Actuator health and metrics
    - Configure `/actuator/health` to check: database, email inbox, LLM provider availability
    - 5s timeout per dependency health check, DOWN if timeout
    - Configure `/actuator/metrics` to report: jobs ingested, jobs matched, applications prepared, active workflow status, LLM call counts
    - _Requirements: 15.3, 15.4, 15.5_

  - [~] 23.3 Harden input validation and error handling
    - Verify all endpoints have schema validation with field-level errors (400)
    - Verify generic 500 error messages don't expose internals
    - Verify malformed JSON returns 400 with parse error message
    - Verify all error responses follow consistent format (timestamp, status, error, message, path)
    - _Requirements: 13.1, 13.2, 13.3, 13.5, 13.6_

  - [ ]* 23.4 Write integration tests for observability
    - Test correlation ID propagation through request lifecycle
    - Test health endpoint reports UP/DOWN correctly
    - Test metrics endpoint reports expected counters
    - _Requirements: 15.2, 15.3, 15.5_

  - **How to test E2E**: Make API calls and check structured logs (JSON with correlation IDs). Hit `/actuator/health` — see dependency statuses. Hit `/actuator/metrics` — see counters. Send malformed JSON — get consistent 400 error. Send oversized text — get specific validation error.

- [ ] 24. Docker Deployment
  - [~] 24.1 Create Dockerfiles for all services
    - Create `career-agent-service/Dockerfile` — Java 25 runtime, multi-stage build
    - Create `career-agent-ui/Dockerfile` — Node.js 22, build Next.js, serve
    - Create `browser-automation-service/Dockerfile` — Node.js 22, install Playwright + Chromium
    - _Requirements: 17.1, 17.2_

  - [~] 24.2 Finalize Docker Compose configuration
    - Complete `docker-compose.yml` with all 4 services (postgres, api, ui, browser-automation)
    - Health checks for each service (pg_isready, /actuator/health, /api/health, /health)
    - Correct dependency ordering (service_healthy conditions)
    - Named volume for pgdata, bind mount for documents
    - All config via `.env` file
    - Update `.env.example` with all required variables
    - Create `README.md` with setup instructions, env var documentation, and example values
    - _Requirements: 17.3, 17.4, 17.5, 17.6, 17.7_

  - [ ]* 24.3 Write deployment smoke tests
    - Test `docker-compose up` starts all services
    - Test all health endpoints respond
    - Test frontend can reach backend through Docker network
    - _Requirements: 17.3_

  - **How to test E2E**: Run `docker-compose up`. Wait for all health checks to pass. Open `http://localhost:3000` — register, login, browse dashboard. Verify all features work end-to-end through Docker. Check `.env.example` has all documented variables.

- [~] 25. Final Checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at key milestones
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- Every task is a vertical slice testable end-to-end from the browser
- Technologies: Java 25 / Spring Boot 4.1 (backend), TypeScript / Next.js 16 (frontend), TypeScript / Express 5 / Playwright (browser automation), PostgreSQL 17 (database)
- The design document's 26 correctness properties map to property-based test sub-tasks throughout the plan
- Flyway migrations are numbered sequentially (V1 through V9) and introduced with each feature slice

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["1.3"] },
    { "id": 2, "tasks": ["1.4"] },
    { "id": 3, "tasks": ["2.1"] },
    { "id": 4, "tasks": ["2.2", "2.3", "2.4"] },
    { "id": 5, "tasks": ["4.1", "4.2"] },
    { "id": 6, "tasks": ["4.3", "4.4", "4.5", "4.6"] },
    { "id": 7, "tasks": ["6.1"] },
    { "id": 8, "tasks": ["6.2", "6.3"] },
    { "id": 9, "tasks": ["6.4", "6.5", "6.6"] },
    { "id": 10, "tasks": ["7.1", "7.2"] },
    { "id": 11, "tasks": ["7.3", "7.4", "7.5"] },
    { "id": 12, "tasks": ["8.1"] },
    { "id": 13, "tasks": ["8.2", "8.4", "8.5"] },
    { "id": 14, "tasks": ["8.3"] },
    { "id": 15, "tasks": ["10.1"] },
    { "id": 16, "tasks": ["10.2", "10.3", "10.4"] },
    { "id": 17, "tasks": ["11.1"] },
    { "id": 18, "tasks": ["11.2", "11.3"] },
    { "id": 19, "tasks": ["12.1"] },
    { "id": 20, "tasks": ["12.2", "12.3", "12.4"] },
    { "id": 21, "tasks": ["13.1"] },
    { "id": 22, "tasks": ["13.2", "13.3", "13.4"] },
    { "id": 23, "tasks": ["15.1"] },
    { "id": 24, "tasks": ["15.2", "15.3", "15.4"] },
    { "id": 25, "tasks": ["16.1"] },
    { "id": 26, "tasks": ["16.2", "16.3"] },
    { "id": 27, "tasks": ["17.1"] },
    { "id": 28, "tasks": ["17.2", "17.3"] },
    { "id": 29, "tasks": ["18.1", "18.2"] },
    { "id": 30, "tasks": ["18.3", "18.4", "18.5"] },
    { "id": 31, "tasks": ["19.1"] },
    { "id": 32, "tasks": ["19.2", "19.4", "19.5"] },
    { "id": 33, "tasks": ["19.3"] },
    { "id": 34, "tasks": ["21.1"] },
    { "id": 35, "tasks": ["21.2", "21.3"] },
    { "id": 36, "tasks": ["22.1"] },
    { "id": 37, "tasks": ["22.2", "22.3", "22.4"] },
    { "id": 38, "tasks": ["23.1", "23.2"] },
    { "id": 39, "tasks": ["23.3", "23.4"] },
    { "id": 40, "tasks": ["24.1"] },
    { "id": 41, "tasks": ["24.2", "24.3"] }
  ]
}
```

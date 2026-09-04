# Career Agent — Implementation Guide

This document walks through the Career Agent project step by step, explaining what we build at each stage and why. Each section represents a milestone that can be tested end-to-end — from the web interface all the way through to the database.

Every section includes the Git branch name where that work lives, so you can check out any branch to see the project at that stage.

---

## Step 1: Project Foundation

**Branch:** `feat/project-scaffold-health-check`

**What we're doing:** Setting up the two main applications (backend and frontend) from scratch, connecting them to each other, and making sure the basic infrastructure works.

**Why this matters:** Before building any features, we need a solid foundation. Think of this like laying the foundation of a house — the walls and rooms come later, but without a solid base, nothing else works.

**What gets built:**
- **Backend application** (career-agent-service) — A Java/Spring Boot server that will handle all the business logic, AI agents, and data storage. At this stage, it just starts up and reports "I'm healthy."
- **Frontend application** (career-agent-ui) — A Next.js web application that users will interact with through their browser. At this stage, it shows a simple landing page.
- **Database** — A PostgreSQL database running in Docker, ready to store candidate profiles, jobs, and applications.
- **MinIO** — An S3-compatible object storage service running in Docker, used for storing documents and CVs.
- **Docker Compose** — A configuration file that starts all infrastructure services (database, MinIO, etc.) with a single command.
- **Health check connection** — The frontend calls the backend's health endpoint and displays whether the backend is running (green "UP") or not (red "DOWN").

**How to test it:** Start everything up, open `http://localhost:3000` in your browser, and you should see a page showing "Career Agent" with the backend status displayed as "UP" with a green indicator.

---

## Step 2: User Accounts

**Branch:** `feat/user-auth-registration-login`

**What we're doing:** Adding the ability for users to create accounts, log in, and have their identity verified on every request. This is the security layer that protects all personal data.

**Why this matters:** Every feature from this point forward is personal — your profile, your job matches, your applications. We need to know who you are before showing you anything. Without authentication, anyone could see anyone's data.

**What gets built:**
- **Registration** — A form where new users enter their name, email, and password to create an account. Passwords must be at least 8 characters with uppercase, lowercase, and a number.
- **Login** — A form where existing users enter their email and password to sign in. On success, the system issues a secure token (JWT) that proves who you are.
- **Token-based security** — Every request from the browser includes a security token. The backend verifies this token on every request. No valid token = no access.
- **Dashboard shell** — After logging in, users see a dashboard layout with a navigation sidebar (Dashboard, Jobs, Applications, Workflows, Profile, Settings) and a top bar with a logout button.
- **Password security** — Passwords are hashed using BCrypt (industry standard) before storage. Even if the database were compromised, passwords can't be recovered.
- **Error handling** — Clear, consistent error messages for validation failures, wrong passwords, duplicate emails, and other issues.
- **31 automated tests** — Property-based and unit tests covering password validation rules, JWT token lifecycle, authentication flow, and error response format.

**How to test it:** Open `http://localhost:3000` → you're redirected to the login page. Click "Register" → fill in your details → you land on the dashboard. Log out → log back in with the same credentials. Try a weak password → see a helpful error. Try registering with the same email twice → see "Email already registered."

---

## Step 3: Candidate Profile

**Branch:** `feat/candidate-profile-crud`

**What we're doing:** Giving users the ability to create and manage their professional profile — the foundation the AI uses to match them with jobs.

**Why this matters:** The matching AI needs to know who you are: your skills, experience, preferred locations, salary expectations, and the types of roles you're looking for. Without a profile, the system has nothing to match against.

**What gets built:**
- **Profile creation** — Upload your CV (PDF or Word), and the system extracts your professional information automatically using AI.
- **Preference management** — Set your target job titles, preferred locations, remote preferences (multi-select: Remote, Hybrid, On-site, Any), minimum salary, preferred industries, target companies, seniority levels (multi-select: Intern through Executive), and exclusions. All multi-value fields offer predefined suggestions with checkbox selection plus custom free-text entries.
- **Document management** — Upload, replace, and delete CV documents stored in MinIO object storage (up to 5 per profile).
- **Profile editing** — Review and edit everything the AI extracted. All changes are saved and used for future matching.
- **Validation** — Salary must be positive, seniority must be from a defined list, locations can't be empty. All inputs are sanitized to prevent security issues.
- **Vector embedding** — Your profile is converted into a mathematical representation (embedding) and stored in the Qdrant vector database, enabling semantic similarity search later.
- **OKF knowledge bundle** — Your profile is also saved as a set of structured markdown documents (Open Knowledge Format) that AI agents can read and reason about.

**How to test it:** Log in → go to Profile → upload a PDF CV → see extracted data appear → edit your preferences → save. Navigate away and come back — everything persists. Try uploading a 6th document → rejected. Delete your profile → all data is gone.

---

## Step 4: Job Discovery & Ingestion

**Branch:** `feat/job-portal-linkedin-ingestion`

**What we're doing:** Building the system that automatically collects job postings from LinkedIn email alerts and displays them in the application.

**Why this matters:** Instead of manually browsing job boards every day, the system collects jobs for you. You configure your LinkedIn job alerts once, and the system processes them automatically.

**What gets built:**
- **Job portal abstraction** — A pluggable architecture so we can add more job sources (Indeed, Greenhouse, etc.) later without changing core code. LinkedIn is the first.
- **LinkedIn email ingestion** — The system connects to your email inbox (IMAP), finds LinkedIn Job Alert emails, parses out job titles, companies, locations, and URLs.
- **Job storage** — Every job is saved to the database with a status tracking its lifecycle (NEW → ANALYZED → MATCHED → SHORTLISTED → etc.).
- **Duplicate detection by URL** — If the same job appears in multiple emails, only one record is created.
- **Job list page** — A table in the frontend showing all ingested jobs with title, company, location, status, and source. Click any job to see its full details.
- **Pagination** — When you have hundreds of jobs, they're shown page by page (20 per page by default).
- **Status transition rules** — Jobs can only move through valid status changes (e.g., a NEW job can't jump straight to APPLIED). Invalid transitions are rejected with a clear error.

**How to test it:** Configure your email credentials → trigger the ingestion → see jobs appear in the Jobs list. Click a job to see details. Trigger again → no duplicates created. Check pagination with many jobs.

---

## Step 5: Job Normalization & Smart Deduplication

**Branch:** `feat/job-normalization-dedup`

**What we're doing:** Using AI to standardize every job posting into a consistent format, and using both exact matching and semantic similarity to catch duplicate postings.

**Why this matters:** Job postings come from different sources in different formats. One might say "Remote" while another says "Work from anywhere." The AI normalizes everything so comparisons are fair. Semantic deduplication catches duplicates that exact matching misses (e.g., "Senior PM" vs "Senior Product Manager" at the same company).

**What gets built:**
- **Job Analysis AI Agent** — An AI agent that reads raw job postings and extracts structured data: title, company, location, remote type, salary range, skills, requirements.
- **Normalization** — Every field is standardized (e.g., remote type becomes REMOTE, HYBRID, ON_SITE, or UNSPECIFIED). Skills are extracted as a clean list. Dates are standardized to ISO 8601.
- **Semantic deduplication** — Besides exact title+company+location matching, the system uses vector similarity (cosine similarity ≥ 0.95) to flag nearly-identical jobs that might have slightly different wording.
- **Embedding service** — Each job's description is converted into a vector embedding and stored in Qdrant for similarity search.
- **OKF concept documents** — Each normalized job is saved as a structured markdown document with provenance (which AI generated it, when, from what source).
- **LLM resilience** — Rate limiting (30 calls/minute), retry with exponential backoff (3 retries), 120-second timeouts, and a queue of up to 500 pending requests.

**How to test it:** Ingest jobs → see them transition from NEW to ANALYZED on the jobs page. Verify normalized fields display (remote type badges, skill tags). Ingest the same jobs again → verify duplicates are merged, not created.

---

## Step 6: AI Job Matching

**Branch:** `feat/ai-job-matching`

**What we're doing:** The core intelligence — scoring every job against your profile and explaining why it's a good or bad fit.

**Why this matters:** This is the feature that saves hours every day. Instead of reading 100 job postings, the system tells you "these 15 are worth your time" and explains exactly why for each one.

**What gets built:**
- **Matching AI Agent** — An AI agent that compares each job against your profile and produces a detailed match result.
- **Multi-dimensional scoring** — Each job gets scored 0-100 on five dimensions: skills match, experience match, location match, salary match, and seniority match. The overall score is a weighted combination.
- **Explanations** — For each match: strengths (why you're a good fit), gaps (where you fall short), and risks (potential concerns).
- **Smart pre-ranking** — Before running the expensive AI analysis, the system uses Qdrant vector similarity to quickly filter out clearly irrelevant jobs (cosine similarity < 0.3), saving AI API costs.
- **Configurable threshold** — You set your minimum match score (default: 60). Jobs scoring above are shortlisted; below are skipped.
- **OKF audit trail** — Each match is recorded as an "Attested Computation" in OKF format, documenting exactly how the score was produced (the prompt, the model, the inputs) for full auditability.
- **Match result display** — The job detail page shows the full breakdown: dimension scores, strengths, gaps, risks, and an overall recommendation.

**How to test it:** Ensure your profile is complete → trigger the workflow → see jobs scored on the jobs page. Click a job → see the full match breakdown. Change the threshold in settings → verify different results.

---

## Step 7: Shortlist Dashboard

**Branch:** `feat/shortlist-dashboard`

**What we're doing:** Building the main dashboard where you review your best job matches and take action on them.

**Why this matters:** This is the daily command center — the page you open every morning to see what new opportunities are worth pursuing.

**What gets built:**
- **Ranked job list** — Shortlisted jobs displayed by match score (highest first) with title, company, location, remote type, score, and match summary.
- **Filters** — Filter by minimum score, location, company, keyword, status, and date range.
- **Summary counts** — At-a-glance numbers: total jobs ingested, shortlisted, skipped, and applications in progress.
- **Actions** — For each job: "Prepare Application" (generate materials), "Skip" (hide it), "View Details" (full breakdown).
- **Skip & Restore** — Skipped jobs move to a separate tab. Changed your mind? Click "Restore" to bring them back.
- **Empty states** — Clear messaging when no jobs match your filters.
- **Pagination** — Configurable page size (1-100, default 20).

**How to test it:** Log in → dashboard shows shortlisted jobs ranked by score. Apply filters → list updates. Skip a job → it moves to the skipped tab. Restore it. View the summary counts. Click "View Details" to see the full match result.

---

## Step 8: Application Package Preparation

**Branch:** `feat/application-package-prep`

**What we're doing:** Using AI to generate a tailored application package (CV recommendations, cover letter, screening answers) for each job you want to apply to.

**Why this matters:** Writing a custom cover letter for every job is time-consuming. The AI generates a first draft tailored to each specific job, highlighting your strengths and addressing gaps identified in the match analysis.

**What gets built:**
- **Application AI Agent** — Generates a CV recommendation (how to tailor your CV for this role), a customized cover letter, and answers to up to 5 common screening questions.
- **Review & Edit** — Everything is presented for your review. Edit the cover letter, adjust screening answers, tweak the CV recommendation before approving.
- **Approval flow** — Click "Approve" to finalize. The job status changes to READY_TO_APPLY.
- **Retry on failure** — If the AI fails (timeout, error), you can retry with one click.
- **Version tracking** — Every edit is saved as a versioned document.
- **OKF provenance** — Each generated document tracks which AI agent created it and when, updated to "human-reviewed" when you approve.

**How to test it:** From the shortlist → click "Prepare Application" → wait for generation → see CV recommendation, cover letter, screening answers. Edit the cover letter → save → approve. Verify the job status changes to READY_TO_APPLY.

---

## Step 9: Application Tracking

**Branch:** `feat/application-tracking`

**What we're doing:** Tracking every application through its complete lifecycle — from preparation to offer (or rejection).

**Why this matters:** When you're applying to many jobs, it's easy to lose track. This gives you a single view of every application's status, when it was submitted, and what happened.

**What gets built:**
- **Status lifecycle** — NEW → ANALYZED → MATCHED → SHORTLISTED → APPLICATION_PREPARED → READY_TO_APPLY → APPLIED → INTERVIEW → OFFER → CLOSED. Plus side statuses: REJECTED, SKIPPED, EXPIRED, SUBMISSION_FAILED.
- **Mark as Applied** — After applying manually, click "Mark as Applied" to record the date and documents used.
- **Status updates** — Update status as you progress (INTERVIEW → OFFER → ACCEPTED or REJECTED).
- **Tracking view** — Table showing all applications: company, position, applied date, match score, current status, days since last change.
- **Transition validation** — Can't jump from APPLIED straight to CLOSED. Invalid transitions show which statuses are valid next.
- **History** — Every status change is timestamped for a complete audit trail.

**How to test it:** Applications page → see tracked applications. Mark a READY_TO_APPLY job as Applied. Update to INTERVIEW → OFFER. Try an invalid transition → see the error with valid options.

---

## Step 10: Automated Workflow

**Branch:** `feat/scheduled-workflow`

**What we're doing:** Making the entire pipeline (ingest → normalize → deduplicate → match → shortlist) run automatically on a schedule.

**Why this matters:** Instead of manually triggering each step, the system runs the complete pipeline every morning at 8:00 AM. You wake up to a fresh shortlist of matched jobs.

**What gets built:**
- **Scheduled execution** — Configurable cron schedule (default: daily at 08:00 UTC).
- **Manual trigger** — A "Run Now" button on the Workflows page for on-demand execution.
- **Execution tracking** — Every run is recorded: when it started, when it finished, how many jobs were ingested, deduplicated, matched, and shortlisted.
- **Failure handling** — If one stage fails, the system logs the error, skips that stage, and continues with the rest.
- **Mutual exclusion** — Only one workflow can run at a time. Clicking "Run Now" while one is running shows "already in progress."

**How to test it:** Workflows page → click "Run Now" → see execution appear as RUNNING → completes with counts. Click again while running → see rejection message. Verify new jobs appear on the shortlist.

---

## Step 11: Company Career Pages

**Branch:** `feat/career-page-ingestion`

**What we're doing:** Adding a second job source — direct career page scraping from your target companies.

**Why this matters:** Not all jobs appear on LinkedIn. By monitoring specific company career pages, you catch opportunities that only exist on the company's own website.

**What gets built:**
- **URL management** — Add, edit, and remove target company career page URLs (up to 50).
- **Career page scraper** — Fetches and parses job listings from configured URLs.
- **URL validation** — Must be HTTPS, valid URL format, max 2048 characters.
- **Integration with pipeline** — Scraped jobs flow through the same normalize → deduplicate → match pipeline as LinkedIn jobs.

**How to test it:** Settings → add a career page URL (try HTTP → rejected, HTTPS → accepted). Trigger workflow → see jobs from career pages appear. Add an invalid URL → validation error. Try adding a 51st → rejected.

---

## Step 12: Application Mode Configuration

**Branch:** `feat/application-mode-config`

**What we're doing:** Adding the toggle between manual application and automatic LinkedIn application.

**Why this matters:** Some users want full control (apply manually), others want maximum automation (system applies for you). This setting controls which path is taken after you approve an application package.

**What gets built:**
- **MANUAL / AUTO_APPLY toggle** — Global setting on your profile.
- **Per-job override** — Override the global setting for individual jobs.
- **Pre-submit review toggle** — When auto-applying, choose whether to review the filled form before submission.
- **Match score threshold** — Adjustable slider (1-100) for the minimum match score to shortlist.

**How to test it:** Settings → toggle to AUTO_APPLY → save. Override a specific job to MANUAL. Change threshold → verify different shortlist results.

---

## Step 13: Browser Automation Service

**Branch:** `feat/browser-automation-service`

**What we're doing:** Setting up a separate service that can control a real web browser to fill and submit job applications automatically.

**Why this matters:** Auto-applying to LinkedIn jobs requires a real browser that can navigate pages, fill forms, click buttons, and upload files — just like a human would.

**What gets built:**
- **Playwright service** — A Node.js/Express service running Playwright (headless Chrome).
- **Portal plugin architecture** — Pluggable handlers for different job portals (LinkedIn first, others later).
- **Rate limiting** — Maximum 10 submissions per hour, 30 per day to avoid account restrictions.
- **Health endpoint** — Reports service status, browser state, and submission counts.

**How to test it:** Start the service → hit `http://localhost:4000/health` → see status, browser state, submission counts.

---

## Step 14: Portal Session Management

**Branch:** `feat/portal-session-management`

**What we're doing:** Securely storing your LinkedIn login session so the automation service can act on your behalf without repeatedly asking for credentials.

**Why this matters:** The browser automation needs to be logged into LinkedIn to submit applications. This step handles authentication securely — we never store your password, only the session cookies, and those are encrypted.

**What gets built:**
- **Browser-based login** — Click "Connect LinkedIn" → a browser opens → you log in normally → session is captured and encrypted.
- **AES-256 encryption** — Session cookies are encrypted at rest.
- **30-day TTL** — Sessions auto-expire after 30 days, requiring re-authentication.
- **Session validation** — Before each submission, the session is verified against LinkedIn.
- **Disconnect** — Revoke access with one click (deletes encrypted data within 5 seconds).

**How to test it:** Settings → click "Connect LinkedIn" → login page opens → enter credentials → see "Connected" with expiry date. Click "Disconnect" → status returns to disconnected.

---

## Step 15: LinkedIn Auto-Apply

**Branch:** `feat/linkedin-auto-apply`

**What we're doing:** The main automation — automatically filling and submitting LinkedIn Easy Apply applications using your approved materials.

**Why this matters:** This is the culmination of the entire system. The AI found the best jobs, prepared your materials, you approved them — now the system submits the application for you.

**What gets built:**
- **Easy Apply detection** — Identifies whether a job supports LinkedIn's one-click apply vs redirecting to an external site.
- **Form filling** — Automatically uploads your CV, enters cover letter text, fills screening question answers.
- **Multi-step forms** — Handles up to 10-step application flows.
- **Unmatched questions** — If a screening question wasn't prepared for, the system pauses and asks you to answer it.
- **External redirect fallback** — Jobs that redirect to external sites fall back to MANUAL mode.
- **Success/failure detection** — Confirms submission via LinkedIn's confirmation page.

**How to test it:** Set AUTO_APPLY mode → connect LinkedIn → approve an application → see status update to APPLIED (or SUBMISSION_FAILED with screenshot).

---

## Step 16: Pre-Submit Review

**Branch:** `feat/pre-submit-review`

**What we're doing:** Adding an optional review step where you can see exactly what the automation is about to submit before it clicks "Submit."

**Why this matters:** Even with automation, you might want to double-check what's being sent. The pre-submit review shows you a screenshot of the filled form and all the details, giving you final approval control.

**What gets built:**
- **Form screenshot** — After filling all fields, the system takes a screenshot before submitting.
- **Review summary** — Shows job title, company, CV being uploaded, cover letter, screening answers, and the form screenshot.
- **Approve / Reject** — Approve to submit, or reject to go back and edit.
- **24-hour timeout** — If you don't respond within 24 hours, the submission is cancelled.
- **Notification badge** — Dashboard shows how many applications are waiting for your review.

**How to test it:** Enable Pre-Submit Review → approve an application → see notification badge → review the summary with screenshot → approve to submit (or reject to edit).

---

## Step 17: Submission Retry & Failure Handling

**Branch:** `feat/submission-result-retry`

**What we're doing:** Handling what happens when an auto-apply attempt fails — screenshots, retry logic, CAPTCHA handling.

**Why this matters:** Automated submissions can fail for many reasons: page changes, CAPTCHAs, timeouts. The system needs to handle these gracefully, give you clear information, and let you retry or switch to manual.

**What gets built:**
- **Failure detection** — Screenshots captured on failure for diagnosis.
- **Retry logic** — Up to 3 retry attempts per job. After 3 failures, auto-apply is blocked for that job.
- **CAPTCHA handling** — If LinkedIn shows a CAPTCHA, the system pauses and notifies you to solve it (5-minute timeout).
- **Attempt history** — Every submission attempt recorded with timestamp, result, and failure reason.
- **Switch to manual** — After max retries, prompted to switch to manual application.

**How to test it:** Trigger auto-apply on a job. If it fails, see SUBMISSION_FAILED with screenshot. Click "Retry" → see attempt count. After 3 failures, "Switch to Manual" appears.

---

## Step 18: Observability & Production Hardening

**Branch:** `feat/observability-production`

**What we're doing:** Adding the monitoring, logging, and error handling needed to run the system reliably in production.

**Why this matters:** When something goes wrong in production, you need to quickly find and fix it. Structured logging, health checks, and metrics make the system observable and diagnosable.

**What gets built:**
- **Structured JSON logging** — Every log entry includes a timestamp, level, correlation ID, and context.
- **Correlation IDs** — Each request gets a unique ID that traces through all log entries, making it easy to follow a request's entire journey.
- **Health checks** — `/actuator/health` reports status of PostgreSQL, Qdrant, email, and LLM provider.
- **Metrics** — `/actuator/metrics` reports jobs ingested, matched, applications prepared, LLM call counts.
- **Input validation hardening** — All endpoints verified for proper validation, consistent error format, no internal details leaked in 500 errors.

**How to test it:** Make API calls → check structured logs. Hit `/actuator/health` → see dependency statuses. Send malformed JSON → get consistent 400 error.

---

## Step 19: Docker Deployment

**Branch:** `feat/docker-deployment`

**What we're doing:** Packaging everything into Docker containers so the entire system can be started with a single command.

**Why this matters:** For production deployment or sharing with others, everything needs to run consistently regardless of the host machine. Docker makes the system portable and reproducible.

**What gets built:**
- **Backend Dockerfile** — Java 25 multi-stage build producing a minimal container.
- **Frontend Dockerfile** — Node.js build + static asset serving.
- **Browser automation Dockerfile** — Node.js + Playwright + Chromium.
- **Complete Docker Compose** — All 5 services (PostgreSQL, Qdrant, backend, frontend, browser automation) with health checks, dependency ordering, and named volumes.
- **Documentation** — `.env.example` with all variables, README with setup instructions.

**How to test it:** Run `docker compose up` → wait for health checks → open `http://localhost:3000` → register, login, browse dashboard. Everything works end-to-end through Docker.

---

## Summary

| Step | What | Branch | Status |
|---|---|---|---|
| 1 | Project Foundation | `feat/project-scaffold-health-check` | ✅ Complete |
| 2 | User Accounts | `feat/user-auth-registration-login` | ✅ Complete |
| 3 | Candidate Profile | `feat/candidate-profile-crud` | ⬜ Planned |
| 4 | Job Discovery | `feat/job-portal-linkedin-ingestion` | ⬜ Planned |
| 5 | Normalization & Dedup | `feat/job-normalization-dedup` | ⬜ Planned |
| 6 | AI Matching | `feat/ai-job-matching` | ⬜ Planned |
| 7 | Shortlist Dashboard | `feat/shortlist-dashboard` | ⬜ Planned |
| 8 | Application Preparation | `feat/application-package-prep` | ⬜ Planned |
| 9 | Application Tracking | `feat/application-tracking` | ⬜ Planned |
| 10 | Automated Workflow | `feat/scheduled-workflow` | ⬜ Planned |
| 11 | Company Career Pages | `feat/career-page-ingestion` | ⬜ Planned |
| 12 | Application Mode Config | `feat/application-mode-config` | ⬜ Planned |
| 13 | Browser Automation | `feat/browser-automation-service` | ⬜ Planned |
| 14 | Session Management | `feat/portal-session-management` | ⬜ Planned |
| 15 | LinkedIn Auto-Apply | `feat/linkedin-auto-apply` | ⬜ Planned |
| 16 | Pre-Submit Review | `feat/pre-submit-review` | ⬜ Planned |
| 17 | Retry & Failure Handling | `feat/submission-result-retry` | ⬜ Planned |
| 18 | Observability | `feat/observability-production` | ⬜ Planned |
| 19 | Docker Deployment | `feat/docker-deployment` | ⬜ Planned |

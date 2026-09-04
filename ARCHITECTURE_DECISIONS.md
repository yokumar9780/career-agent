# Architecture Decision Records (ADRs)

This document captures the key architectural decisions made during the Career Agent project, along with the context, alternatives considered, and reasoning behind each choice. ADRs are numbered sequentially and are append-only — decisions are never deleted, only superseded by newer ADRs if the direction changes.

For the full implementation walkthrough, see the **[Implementation Guide](./IMPLEMENTATION_GUIDE.md)**.

---

## ADR-001: LinkedIn Email Ingestion Over LinkedIn API

**Status:** Accepted  
**Date:** 2026-09-04  
**Relates to:** Task 6 (Job Portal Abstraction & LinkedIn Email Ingestion)

### Context

The Career Agent needs to ingest job postings from LinkedIn as the primary job source for the MVP. The natural first question is whether LinkedIn provides an API for searching and fetching job listings.

### Alternatives Considered

| Approach | Feasibility | Risk | Cost |
|----------|------------|------|------|
| **LinkedIn Job Search API** | Not available | N/A | N/A |
| **LinkedIn Talent Solutions API** | Requires paid Recruiter/Job Slots subscription + LinkedIn Partner application (weeks/months approval, registered business entity required) | Low once approved | High — enterprise pricing |
| **LinkedIn scraping (Playwright/Puppeteer)** | Works short-term | High — LinkedIn actively detects automation via browser fingerprinting, rate limiting, CAPTCHA. Accounts get restricted or banned. | Free |
| **LinkedIn Email Alerts + IMAP parsing** | Reliable, no TOS risk | Low — standard email protocol | Free |

### Decision

Use **LinkedIn Job Alert email ingestion via IMAP** as the primary LinkedIn job source.

### Rationale

1. **No free API exists.** LinkedIn does not offer any public or free API for searching or fetching job listings with a personal account. The available APIs (Marketing API, Talent Solutions API, Apply Connect API) serve different purposes and require paid enterprise subscriptions.

2. **Scraping is unsustainable.** LinkedIn aggressively detects automated browsing — browser fingerprinting, behavioral analysis, rate limiting, and CAPTCHA challenges. Accounts used for scraping regularly get restricted or permanently banned. This is not acceptable for a production system managing a user's real LinkedIn account.

3. **Email alerts are free and reliable.** LinkedIn sends job alert emails to any user who configures them. These emails contain structured HTML with job titles, companies, locations, and URLs. Parsing via IMAP is a standard, well-supported protocol with no TOS risk for the ingestion side.

4. **Frequency is adequate.** LinkedIn sends alerts daily or weekly depending on configuration. Users can set up multiple alerts with different criteria. While not real-time, the daily workflow schedule aligns well with email alert frequency.

### Consequences

- Users must configure LinkedIn Job Alerts for their target criteria (one-time setup).
- Job ingestion is not real-time — there is a delay between a job being posted and it appearing in an email alert.
- The JobSourceAdapter interface makes it trivial to add API-based sources later if LinkedIn ever opens access or if we add other portals (Indeed, Glassdoor) that do have APIs.

### Related ADR

See [ADR-002](#adr-002-browser-automation-for-linkedin-auto-apply) for the auto-apply side of LinkedIn integration.

---

## ADR-002: Browser Automation for LinkedIn Auto-Apply

**Status:** Accepted  
**Date:** 2026-09-04  
**Relates to:** Tasks 17-19 (Browser Automation, Session Management, LinkedIn Auto-Apply)

### Context

After identifying and matching jobs, the Career Agent needs to submit applications on LinkedIn. Since no free LinkedIn API exists for job applications either (same restriction as ADR-001), an alternative approach is needed.

### Alternatives Considered

| Approach | Feasibility | Notes |
|----------|------------|-------|
| **LinkedIn Apply API** | Not available for personal use | The Apply Connect API is for job boards to embed "Apply with LinkedIn" buttons — not for submitting applications programmatically. |
| **Manual-only submission** | Works, but defeats the purpose | Users would prepare materials in the app and then manually copy/paste into LinkedIn. |
| **Browser automation (Playwright)** | Works — industry-standard approach | Used by commercial LinkedIn automation tools. Requires careful rate limiting and session management to avoid account restrictions. |

### Decision

Use **Playwright browser automation** running in a separate Node.js service to submit LinkedIn Easy Apply applications.

### Rationale

1. **No API alternative.** LinkedIn provides no API for submitting job applications with a personal account. Browser automation is the only viable approach for auto-apply functionality.

2. **Industry-standard pattern.** Commercial LinkedIn automation tools and open-source projects use the same browser automation approach. The technique is well-understood with known risks and mitigations.

3. **Isolated failure domain.** Running Playwright in a separate service (rowser-automation-service) prevents browser crashes, memory leaks, or hangs from affecting the core Spring Boot API.

4. **Comprehensive safety measures.** The design includes multiple layers of protection:
   - **Rate limiting:** 10 submissions/hour, 30/day (matches human application rates)
   - **Randomized delays:** 30-90 seconds between submissions (mimics human behavior)
   - **Pre-submit review:** Optional pause before clicking submit, showing screenshot for candidate approval
   - **CAPTCHA handling:** Pauses and notifies candidate to solve manually (5-minute timeout)
   - **External redirect detection:** Falls back to MANUAL mode for non-Easy Apply jobs
   - **Retry limits:** Maximum 3 retries, then switches to manual
   - **Session encryption:** AES-256-GCM for stored cookies, 30-day TTL with auto-cleanup

5. **No credential storage.** The system never stores LinkedIn username/password. The candidate logs in through a real browser window, and only encrypted session cookies are retained.

### Consequences

- LinkedIn UI changes may break form-filling selectors — this requires ongoing maintenance.
- There is inherent risk of account restrictions with any automation, though rate limiting and delays minimize this.
- The MANUAL application mode is always available as a fallback.
- The PortalSubmissionHandler interface means new portal handlers (Indeed, Greenhouse) can be added without modifying the core automation service.

---

## ADR-003: Dual-Database Architecture (PostgreSQL + Qdrant)

**Status:** Accepted  
**Date:** 2026-09-01  
**Relates to:** Task 7 (Job Normalization & Deduplication), Task 8 (AI Job Matching)

### Context

The Career Agent needs both traditional relational data storage (users, jobs, applications, status tracking) and vector similarity search (semantic job matching, fuzzy deduplication).

### Alternatives Considered

| Approach | Pros | Cons |
|----------|------|------|
| **PostgreSQL + pgvector** | Single database, simpler ops | pgvector performance degrades at scale, limited indexing options, ties vector search to relational DB scaling |
| **PostgreSQL + Qdrant** | Purpose-built vector engine, independent scaling, richer filtering | Two databases to manage |
| **Qdrant only** | Best vector performance | Cannot replace relational queries, transactions, Flyway migrations |

### Decision

Use **PostgreSQL for relational data** and **Qdrant as a dedicated vector database** for embeddings and similarity search.

### Rationale

1. **Purpose-built engines.** Qdrant is optimized for vector similarity search with advanced indexing (HNSW), payload filtering, and efficient memory management. PostgreSQL with pgvector is a bolt-on that lacks these optimizations.

2. **Independent scaling.** Vector search load (embedding generation, similarity queries) can scale independently from transactional database load.

3. **Spring AI abstraction.** Both are accessed through Spring AI's VectorStore interface, making the vector DB swappable (Pinecone, Weaviate, Milvus) without application code changes.

4. **Graceful degradation.** If Qdrant is unavailable, the system falls back to LLM-only matching and exact-match deduplication — the core workflow continues.

### Consequences

- Two databases to deploy and monitor (mitigated by Docker Compose orchestration).
- Data consistency between PostgreSQL and Qdrant is eventual (embeddings generated async after normalization).

---

## ADR-004: MinIO Object Storage Over Local Filesystem

**Status:** Accepted  
**Date:** 2026-09-02  
**Relates to:** Task 4 (Candidate Profile CRUD — Document Management)

### Context

Candidates upload CV documents (PDF, DOC, DOCX) that need to be stored and retrieved. The system also stores OKF Knowledge Bundles and application screenshots.

### Alternatives Considered

| Approach | Pros | Cons |
|----------|------|------|
| **Local filesystem** | Simplest setup | Not portable, lost on container restart without bind mounts, no pre-signed URLs, no multi-instance support |
| **MinIO (S3-compatible)** | S3 API, Docker-native, pre-signed URLs, named volumes | Extra service to run |
| **AWS S3 / Azure Blob** | Production-grade | Requires cloud account, not free for local dev |

### Decision

Use **MinIO** as the document storage backend, accessed through an ObjectStorageService interface.

### Rationale

1. **S3-compatible API.** MinIO implements the S3 API, so switching to AWS S3 or Azure Blob in production requires only changing the configuration — not the code.

2. **Interface abstraction.** The ObjectStorageService interface decouples storage logic from the implementation. MinioStorageService implements it for local/dev; a future S3StorageService would implement the same interface for production.

3. **Docker-native.** MinIO runs as a Docker Compose service with a named volume, health check, and web console — consistent with the rest of the infrastructure.

### Consequences

- Additional Docker service to run locally (mitigated by docker compose up -d minio).
- MinIO console available at localhost:9001 for debugging storage issues.

---

## ADR-005: MUI v9 Over Tailwind CSS / shadcn

**Status:** Accepted  
**Date:** 2026-09-01  
**Relates to:** All frontend tasks

### Context

The frontend needs a component library for consistent, accessible UI components.

### Decision

Use **Material UI (MUI) v9** as the sole design system. No Tailwind CSS, shadcn/ui, or Lucide icons.

### Rationale

1. **Comprehensive component library.** MUI provides pre-built components for everything needed: Autocomplete with checkboxes, DataGrid, Dialogs, Tabs, Sliders, etc.

2. **Accessibility built-in.** MUI components follow WAI-ARIA patterns out of the box.

3. **Single design language.** Using one library avoids style conflicts and reduces bundle size compared to mixing libraries.

### Consequences

- All frontend styling uses MUI's sx prop or styled() — no utility classes.
- Dependencies removed: 	ailwindcss, @tailwindcss/postcss, lucide-react.

---

## ADR-006: Controller → Service → Repository Layered Architecture

**Status:** Accepted  
**Date:** 2026-09-02  
**Relates to:** All backend tasks

### Context

The backend needs a clear separation of concerns to keep code maintainable as the feature set grows.

### Decision

Enforce a strict **Controller → Service → Repository** layering. Controllers never call repositories directly. Business logic lives exclusively in services.

### Rationale

1. **Testability.** Services can be unit-tested with mocked repositories. Controllers can be tested with mocked services. No tight coupling.

2. **Transaction boundaries.** @Transactional belongs on services, not controllers. This pattern makes transaction scope explicit.

3. **Reusability.** Multiple controllers (or scheduled jobs) can call the same service without duplicating logic.

### Consequences

- Every feature requires three layers: DTO → Controller → Service → Repository → Entity.
- Enforced via AGENTS.md rules and code review.

---

## ADR-007: Open Knowledge Format (OKF v0.2) for AI Artifacts

**Status:** Accepted  
**Date:** 2026-09-01  
**Relates to:** Tasks 7, 8, 11 (Normalization, Matching, Application Preparation)

### Context

The system generates AI artifacts (job analyses, match results, cover letters) that need provenance tracking, trust management, and portability.

### Decision

Use **OKF v0.2** as the internal knowledge representation format for all AI-generated artifacts.

### Rationale

1. **Provenance.** Every AI-generated artifact records which agent created it, when, and from what inputs — enabling full auditability.

2. **Trust tiers.** Artifacts progress from unverified → machine-confirmed → human-reviewed, giving clear confidence levels.

3. **Portability.** OKF documents are plain markdown with YAML frontmatter — browsable in any editor, diffable in Git, indexable by search tools.

4. **Agent interoperability.** All four agents (Profile, Job Analysis, Matching, Application) read and write the same format, enabling chain-of-thought workflows.

### Consequences

- Additional storage overhead (OKF bundles alongside database records).
- OKF bundles stored in MinIO, organized per-candidate.

---

## Index

| ADR | Title | Status |
|-----|-------|--------|
| [001](#adr-001-linkedin-email-ingestion-over-linkedin-api) | LinkedIn Email Ingestion Over LinkedIn API | Accepted |
| [002](#adr-002-browser-automation-for-linkedin-auto-apply) | Browser Automation for LinkedIn Auto-Apply | Accepted |
| [003](#adr-003-dual-database-architecture-postgresql--qdrant) | Dual-Database Architecture (PostgreSQL + Qdrant) | Accepted |
| [004](#adr-004-minio-object-storage-over-local-filesystem) | MinIO Object Storage Over Local Filesystem | Accepted |
| [005](#adr-005-mui-v9-over-tailwind-css--shadcn) | MUI v9 Over Tailwind CSS / shadcn | Accepted |
| [006](#adr-006-controller--service--repository-layered-architecture) | Controller → Service → Repository Layered Architecture | Accepted |
| [007](#adr-007-open-knowledge-format-okf-v02-for-ai-artifacts) | Open Knowledge Format (OKF v0.2) for AI Artifacts | Accepted |

-- Job table
CREATE TABLE job (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL REFERENCES candidate_profile(id) ON DELETE CASCADE,
    title VARCHAR(300),
    company VARCHAR(300),
    location VARCHAR(300),
    remote_type VARCHAR(20),
    salary_range VARCHAR(100),
    description TEXT,
    requirements TEXT[],
    skills TEXT[],
    primary_url VARCHAR(2048),
    source_urls TEXT[],
    source_types TEXT[],
    portal_identifier VARCHAR(50),
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    posted_date DATE,
    ingested_at TIMESTAMP NOT NULL DEFAULT NOW(),
    status_changed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_job_candidate ON job(candidate_id);
CREATE INDEX idx_job_status ON job(status);
CREATE INDEX idx_job_primary_url ON job(primary_url);
CREATE INDEX idx_job_company_title_location ON job(company, title, location);

-- Job status history table
CREATE TABLE job_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES job(id) ON DELETE CASCADE,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    reason VARCHAR(500),
    changed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_job_status_history_job ON job_status_history(job_id);

-- Workflow execution table
CREATE TABLE workflow_execution (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL REFERENCES candidate_profile(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    trigger_type VARCHAR(20) NOT NULL,
    jobs_ingested INT DEFAULT 0,
    duplicates_detected INT DEFAULT 0,
    jobs_matched INT DEFAULT 0,
    jobs_shortlisted INT DEFAULT 0,
    error_description TEXT,
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP
);

CREATE INDEX idx_workflow_execution_candidate ON workflow_execution(candidate_id);

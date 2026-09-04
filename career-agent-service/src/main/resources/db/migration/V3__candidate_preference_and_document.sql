CREATE TABLE candidate_preference (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL UNIQUE REFERENCES candidate_profile(id) ON DELETE CASCADE,
    target_job_titles TEXT[] DEFAULT '{}',
    preferred_locations TEXT[] DEFAULT '{}',
    remote_preference VARCHAR(20) DEFAULT 'ANY',
    min_salary NUMERIC(15, 2),
    preferred_industries TEXT[] DEFAULT '{}',
    target_companies TEXT[] DEFAULT '{}',
    seniority_level VARCHAR(20),
    must_have_requirements TEXT[] DEFAULT '{}',
    exclusions TEXT[] DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_candidate_preference_candidate ON candidate_preference(candidate_id);

CREATE TABLE candidate_document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL REFERENCES candidate_profile(id) ON DELETE CASCADE,
    filename VARCHAR(500) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR(1000) NOT NULL,
    extracted_text TEXT,
    primary_cv BOOLEAN NOT NULL DEFAULT false,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_candidate_document_candidate ON candidate_document(candidate_id);

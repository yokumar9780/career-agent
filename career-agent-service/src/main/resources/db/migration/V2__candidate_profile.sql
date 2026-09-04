CREATE TABLE candidate_profile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(200) NOT NULL,
    phone VARCHAR(50),
    summary TEXT,
    application_mode VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    pre_submit_review VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    match_score_threshold INTEGER NOT NULL DEFAULT 60,
    timezone VARCHAR(50) DEFAULT 'UTC',
    schedule_cron VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_candidate_profile_email ON candidate_profile(email);

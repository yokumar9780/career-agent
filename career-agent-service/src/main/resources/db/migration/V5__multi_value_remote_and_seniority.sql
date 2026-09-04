-- Convert remote_preference from single VARCHAR to TEXT array
ALTER TABLE candidate_preference DROP COLUMN remote_preference;
ALTER TABLE candidate_preference ADD COLUMN remote_preferences TEXT[] DEFAULT '{}';

-- Convert seniority_level from single VARCHAR to TEXT array
ALTER TABLE candidate_preference DROP COLUMN seniority_level;
ALTER TABLE candidate_preference ADD COLUMN seniority_levels TEXT[] DEFAULT '{}';

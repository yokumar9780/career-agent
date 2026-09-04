-- Fix content_type column length: DOCX MIME type exceeds VARCHAR(50)
ALTER TABLE candidate_document ALTER COLUMN content_type TYPE VARCHAR(255);

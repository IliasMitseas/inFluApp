-- PostgreSQL SQL Script
-- Add missing metric columns to social_media table
-- Execute this script: psql -U postgres -d infludb -f add_metrics_columns.sql

BEGIN TRANSACTION;

-- Add columns if they don't exist
ALTER TABLE social_media
ADD COLUMN IF NOT EXISTS average_comments INTEGER DEFAULT 0;

ALTER TABLE social_media
ADD COLUMN IF NOT EXISTS average_likes INTEGER DEFAULT 0;

ALTER TABLE social_media
ADD COLUMN IF NOT EXISTS engagement_rate DOUBLE PRECISION DEFAULT 0.0;

ALTER TABLE social_media
ADD COLUMN IF NOT EXISTS profile_views INTEGER DEFAULT 0;

-- Verify columns were created
SELECT column_name, data_type, column_default
FROM information_schema.columns
WHERE table_name = 'social_media'
ORDER BY ordinal_position;

COMMIT;

-- Script complete!


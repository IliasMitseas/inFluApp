#!/bin/bash
# Script to add missing columns to social_media table in PostgreSQL

echo "Adding missing columns to social_media table..."

# Connect to PostgreSQL and run SQL commands
psql -U postgres -d infludb -h localhost << EOF

-- Step 1: Check if columns exist and add them
ALTER TABLE IF EXISTS social_media
ADD COLUMN IF NOT EXISTS average_comments INTEGER DEFAULT 0;

ALTER TABLE IF EXISTS social_media
ADD COLUMN IF NOT EXISTS average_likes INTEGER DEFAULT 0;

ALTER TABLE IF EXISTS social_media
ADD COLUMN IF NOT EXISTS engagement_rate DOUBLE PRECISION DEFAULT 0.0;

ALTER TABLE IF EXISTS social_media
ADD COLUMN IF NOT EXISTS profile_views INTEGER DEFAULT 0;

-- Step 2: Verify columns were added
\d social_media

-- Step 3: Show current data
SELECT id, username, platform, followers, average_comments, average_likes, engagement_rate, profile_views
FROM social_media
LIMIT 5;

EOF

echo "Migration complete!"


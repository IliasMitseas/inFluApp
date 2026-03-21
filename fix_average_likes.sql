-- SQL Script to fix average_likes and other missing metrics columns
-- Run this against the PostgreSQL database

-- Step 1: Add missing columns to social_media table if they don't exist
ALTER TABLE IF EXISTS social_media
ADD COLUMN IF NOT EXISTS average_comments INTEGER DEFAULT 0;

ALTER TABLE IF EXISTS social_media
ADD COLUMN IF NOT EXISTS average_likes INTEGER DEFAULT 0;

ALTER TABLE IF EXISTS social_media
ADD COLUMN IF NOT EXISTS engagement_rate DOUBLE PRECISION DEFAULT 0.0;

ALTER TABLE IF EXISTS social_media
ADD COLUMN IF NOT EXISTS profile_views INTEGER DEFAULT 0;

-- Step 2: Update average_likes for all social media accounts
-- This calculates the average total reactions (LIKE, LOVE, WOW, etc.) from all posts
UPDATE social_media sm
SET average_likes = COALESCE(
    (SELECT
        CASE
            WHEN COUNT(p.id) > 0 THEN CAST(SUM(COALESCE(r_count, 0)) / COUNT(p.id) AS INTEGER)
            ELSE 0
        END
    FROM posts p
    LEFT JOIN (
        SELECT post_id, SUM(COALESCE(count, 0)) as r_count
        FROM reactions
        GROUP BY post_id
    ) r ON p.id = r.post_id
    WHERE p.social_media_id = sm.id),
    0
)
WHERE sm.id > 0;

-- Step 3: Update average_comments for all social media accounts
UPDATE social_media sm
SET average_comments = COALESCE(
    (SELECT
        CASE
            WHEN COUNT(DISTINCT p.id) > 0 THEN CAST(
                COALESCE((SELECT COUNT(*) FROM post_comments pc WHERE pc.post_id IN (SELECT id FROM posts WHERE social_media_id = sm.id)), 0)
                / COUNT(DISTINCT p.id)
            AS INTEGER)
            ELSE 0
        END
    FROM posts p
    WHERE p.social_media_id = sm.id),
    0
)
WHERE sm.id > 0;

-- Step 4: Verify the updates
SELECT
    id,
    username,
    platform,
    followers,
    average_comments,
    average_likes,
    engagement_rate,
    profile_views
FROM social_media
ORDER BY average_likes DESC;


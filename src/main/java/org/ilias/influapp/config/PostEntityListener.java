package org.ilias.influapp.config;

import jakarta.persistence.PostUpdate;
import jakarta.persistence.PostPersist;
import org.ilias.influapp.entities.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA EntityListener to update influencer score whenever a post is created or updated
 */
public class PostEntityListener {

    private static final Logger log = LoggerFactory.getLogger(PostEntityListener.class);

    @PostPersist
    @PostUpdate
    public void onPostChange(Post post) {
        try {
            // When a post is created or updated, update the influencer's score
            if (post != null && post.getSocialMedia() != null && post.getSocialMedia().getInfluencer() != null) {
                var influencer = post.getSocialMedia().getInfluencer();
                
                // Update all metrics
                influencer.updateTotalFollowers();
                influencer.updateEngagementRate();
                influencer.updateInfluencerScore();
                
                log.info("✓ Updated influencer score for {} due to post change: score={}", 
                    influencer.getUsername(), influencer.getInfluencerScore());
            }
        } catch (Throwable ex) {
            log.warn("Failed to update influencer score on post change: {}", ex.getMessage());
        }
    }
}


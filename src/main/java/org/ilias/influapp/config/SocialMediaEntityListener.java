package org.ilias.influapp.config;

import jakarta.persistence.PostUpdate;
import jakarta.persistence.PostPersist;
import org.ilias.influapp.entities.SocialMedia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SocialMediaEntityListener {

    private static final Logger log = LoggerFactory.getLogger(SocialMediaEntityListener.class);

    @PostPersist
    @PostUpdate
    public void onSocialMediaChange(SocialMedia socialMedia) {
        try {
            // When social media metrics change, update the influencer's score
            if (socialMedia != null && socialMedia.getInfluencer() != null) {
                var influencer = socialMedia.getInfluencer();
                
                // Update all metrics
                influencer.updateTotalFollowers();
                influencer.updateEngagementRate();
                influencer.updateInfluencerScore();
                
                log.info("✓ Updated influencer score for {} due to {} change: score={}", 
                    influencer.getUsername(), socialMedia.getPlatform(), influencer.getInfluencerScore());
            }
        } catch (Throwable ex) {
            log.warn("Failed to update influencer score on social media change: {}", ex.getMessage());
        }
    }
}


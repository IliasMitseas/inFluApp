package org.ilias.influapp.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Builder
@Setter
@Getter
@Entity(name = "posts")
@AllArgsConstructor
@NoArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;
    private Integer likes;

    @ElementCollection
    @CollectionTable(name = "post_comments", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "comment", length = 2000)
    private List<String> comments;

    private Integer shares;
    private Integer reach;
    private Integer impressionCount;

    @Enumerated(EnumType.STRING)
    private PostSentiment postSentiment;

    private Double engagementRate;

    @ManyToOne(fetch = FetchType.LAZY)
    private SocialMedia socialMedia;

    // Optional: only set for sponsored/deliverable posts
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collaboration_id")
    private Collaboration collaboration;

    public void calculateAndSetEngagementRate() {
        if (impressionCount != null && impressionCount > 0) {
            // Calculate total interactions (simple count - no weights)
            double totalInteractions = 0.0;

            totalInteractions += (likes != null ? likes : 0);
            totalInteractions += (comments != null ? comments.size() : 0);
            totalInteractions += (shares != null ? shares : 0);
            totalInteractions += (reach != null ? reach*0.2 : 0);

            // Standard engagement rate formula: (interactions / impressions) × 100
            double rate = (totalInteractions / impressionCount) * 100.0;

            // Apply sentiment multiplier (more subtle adjustments)
            if (postSentiment != null) {
                switch (postSentiment) {
                    case LOVE -> rate *= 1.2;      // Boost by 20% for LOVE
                    case LIKE -> rate *= 1.1;      // Boost by 10% for LIKE
                    case NEUTRAL -> rate *= 1.0;   // No change
                    case DISLIKE -> rate *= 0.9;   // Reduce by 10% for DISLIKE
                    case TERRIBLE -> rate *= 0.8;  // Reduce by 20% for TERRIBLE
                }
            }

            setEngagementRate(rate);
        } else {
            setEngagementRate(0.0);
        }
    }
}

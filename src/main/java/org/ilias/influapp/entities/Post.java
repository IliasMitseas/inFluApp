package org.ilias.influapp.entities;

import jakarta.persistence.*;
import lombok.*;
import org.ilias.influapp.entities.Enums.PostSentiment;
import org.ilias.influapp.config.PostEntityListener;

import java.util.List;

@Builder
@Setter
@Getter
@Entity(name = "posts")
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(PostEntityListener.class)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String content;
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Reaction> reactions;
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collaboration_id")
    private Collaboration collaboration;

    @OneToOne(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private SentimentAnalysis sentimentAnalysis;

    public Integer getTotalReactions() {
        if (reactions == null || reactions.isEmpty()) {
            return 0;
        }
        return reactions.stream()
                .mapToInt(Reaction::getCount)
                .sum();
    }

    public void calculateAndSetEngagementRate() {
        int totalReactions = getTotalReactions();
        int totalComments = (comments != null) ? comments.size() : 0;
        int totalShares = (shares != null) ? shares : 0;
        double totalInteractions = totalReactions + totalComments + totalShares;

        if (totalInteractions == 0) {
            engagementRate = 0.0;
            return;
        }

        int reachValue = (reach != null) ? reach : 0;
        int impressionsValue = (impressionCount != null) ? impressionCount : 0;

        // Base = max(reach, impressions). These represent the audience size.
        int base = Math.max(reachValue, impressionsValue);

        if (base == 0) {
            // No reach/impressions data → cannot compute a meaningful rate
            engagementRate = 0.0;
            return;
        }

        engagementRate = (totalInteractions / (double) base) * 100.0;

        // Cap at 100% for edge cases where interactions exceed reach
        if (engagementRate > 100.0) {
            engagementRate = 100.0;
        }
    }
}
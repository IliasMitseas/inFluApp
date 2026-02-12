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
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
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

    public Integer getTotalReactions() {
        return reactions.stream()
                .mapToInt(Reaction::getCount)
                .sum();
    }

    public void calculateAndSetEngagementRate() {
        // Συνολικές αλληλεπιδράσεις
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

        // Το base πρέπει να είναι τουλάχιστον όσο τα totalInteractions
        int base = Math.max(Math.max(reachValue, impressionsValue), (int) totalInteractions);

        if (base == 0) {
            engagementRate = 0.0;
            return;
        }

        // Engagement Rate: πάντα <= 100%
        engagementRate = (totalInteractions / base) * 100;

        // safety net για λάθος δεδομένα
        if (engagementRate > 100.0) {
            engagementRate = 100.0;
        }
    }
}
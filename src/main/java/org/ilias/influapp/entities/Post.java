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
        if (impressionCount == null || impressionCount == 0) {
            engagementRate = 0.0;
            return;
        }

        double weightedReactions = reactions.stream()
                .mapToDouble(reaction -> {
                    int count = reaction.getCount();
                    return switch (reaction.getType()) {
                        case LIKE -> count * 1.2;
                        case HAHA, WOW -> count * 1.1;
                        case LOVE -> count * 1.5;
                        case SAD, ANGRY -> count * -0.8;
                    };
                })
                .sum();

        double totalInteractions = reactions.size() + comments.size() + shares + reach * 0.2 + weightedReactions * 0.5;

        engagementRate = (totalInteractions / impressionCount) * 100;
    }
}
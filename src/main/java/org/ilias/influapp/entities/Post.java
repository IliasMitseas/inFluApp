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
    private Integer impressions;

    private Double engagementRate;

    @ManyToOne(fetch = FetchType.LAZY)
    private SocialMedia socialMedia;

    // Optional: only set for sponsored/deliverable posts
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collaboration_id")
    private Collaboration collaboration;

    public void calculateAndSetEngagementRate() {
        if (impressions != null && impressions > 0) {
            double engagementRate = ((likes != null ? likes : 0) +
                    (comments != null ? comments.size() : 0) +
                    (shares != null ? shares : 0))
                    / (double) impressions;
            setEngagementRate(engagementRate * 100); // as percentage
        } else {
            setEngagementRate(0.0);
        }
    }
}

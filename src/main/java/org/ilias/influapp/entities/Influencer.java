package org.ilias.influapp.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.ilias.influapp.entities.Enums.Category;
import org.ilias.influapp.entities.Enums.InfluencerType;
import org.ilias.influapp.entities.Enums.PostSentiment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Transient;

@Entity
@Table(name = "influencers")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Influencer extends User {

    private String name;

    private String age;

    private String location;

    private String bio;

    private Boolean isAvailable;

    private Integer minCollaborationBudget;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Enumerated(EnumType.STRING)
    private InfluencerType influencerType;

    private Integer totalFollowers;

    private BigDecimal engagementRate;

    private Double influencerScore;

    private Double avgPostSentiment;

    @Builder.Default
    @OneToMany(mappedBy = "influencer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SocialMedia> socialMediaAccounts = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "influencer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Collaboration> collaborations = new ArrayList<>();

    public void addSocialMediaAccount(SocialMedia account) {
        if (account == null) {
            return;
        }
        socialMediaAccounts.add(account);
        account.setInfluencer(this);
    }



    public void addCollaboration(Collaboration collaboration) {
        if (collaboration == null) {
            return;
        }
        collaborations.add(collaboration);
        collaboration.setInfluencer(this);
    }


    public void updateEngagementRate() {

        if (socialMediaAccounts == null || socialMediaAccounts.isEmpty()) {
            this.engagementRate = null;
            return;
        }

        double totalEngagementRate = 0.0;
        int totalPosts = 0;

        // Συλλέγουμε όλα τα posts από όλες τις πλατφόρμες
        for (SocialMedia sm : socialMediaAccounts) {
            if (sm != null && sm.getPosts() != null && !sm.getPosts().isEmpty()) {
                for (Post post : sm.getPosts()) {
                    if (post.getEngagementRate() != null) {
                        totalEngagementRate += post.getEngagementRate();
                        totalPosts++;
                    }
                }
            }
        }

        // Υπολογίζουμε μέσο όρο
        if (totalPosts > 0) {
            this.engagementRate = BigDecimal.valueOf(totalEngagementRate / totalPosts);
        } else {
            this.engagementRate = null;
        }
    }



    public int updateTotalFollowers() {
        int total = 0;
        if (socialMediaAccounts != null && !socialMediaAccounts.isEmpty()) {
            for (SocialMedia sm : socialMediaAccounts) {
                if (sm != null && sm.getFollowers() != null) {
                    total += sm.getFollowers();
                }
            }
        }
        this.totalFollowers = total;
        return total;
    }




    public void updateInfluencerScore() {
        double score = 0.0;

        // Engagement component 35%
        if (this.engagementRate != null) {
            double engVal = Math.min(this.engagementRate.doubleValue(), 20.0);
            score += (engVal / 20.0) * 35.0;
        }

        // Followers component 25%
        int followers = this.totalFollowers != null ? this.totalFollowers : 0;
        if (followers > 0) {
            double logFollowers = Math.log10(followers);
            double logMax = Math.log10(1_000_000);
            score += (Math.min(logFollowers, logMax) / logMax) * 25.0;
        }

        // Posts component 20%
        int postCount = 0;
        if (socialMediaAccounts != null) {
            for (SocialMedia sm : socialMediaAccounts) {
                if (sm != null && sm.getPosts() != null) {
                    postCount += sm.getPosts().size();
                }
            }
        }
        score += (Math.min(postCount, 100.0) / 100.0) * 20.0;

        double sentimentSum = 0.0;
        int sentimentCount = 0;

        if (socialMediaAccounts != null) {
            for (SocialMedia sm : socialMediaAccounts) {
                if (sm != null && sm.getPosts() != null) {
                    for (Post post : sm.getPosts()) {
                        if (post.getPostSentiment() != null) {
                            sentimentSum += sentimentToScore(post.getPostSentiment());
                            sentimentCount++;
                        }
                    }
                }
            }
        }
        double avgSentiment = 0.0;
        if (sentimentCount > 0) {
            avgSentiment = sentimentSum / sentimentCount;
            score += ((avgSentiment + 1.0) / 2.0) * 15.0;
            this.avgPostSentiment = Math.round(avgSentiment * 10000.0) / 10000.0; // keep a small scale
        } else {
            this.avgPostSentiment = null;
        }

        // Availability bonus 5%
        if (Boolean.TRUE.equals(this.isAvailable)) {
            score += 5.0;
        }

        this.influencerScore = Math.round(score * 100.0) / 100.0;
    }

    private double sentimentToScore(PostSentiment sentiment) {
        return switch (sentiment) {
            case LOVE -> 0.6;
            case LIKE -> 0.4;
            case NEUTRAL -> 0.0;
            case DISLIKE -> -0.4;
            case TERRIBLE -> -0.6;
        };
    }

    @Transient
    public PostSentiment getAvgPostSentimentLabel() {
        if (this.avgPostSentiment == null) return null;
        double v = this.avgPostSentiment;
        // thresholds are midpoints between numeric mapping in sentimentToScore
        if (v >= 0.5) return PostSentiment.LOVE;
        if (v >= 0.2) return PostSentiment.LIKE;
        if (v > -0.2) return PostSentiment.NEUTRAL;
        if (v >= -0.5) return PostSentiment.DISLIKE;
        return PostSentiment.TERRIBLE;
    }
}

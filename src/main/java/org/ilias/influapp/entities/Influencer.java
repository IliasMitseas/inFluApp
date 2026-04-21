package org.ilias.influapp.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.ilias.influapp.entities.Enums.*;

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

    private static final String INFLUENCER_SCORE_POLICY_VERSION = "v2.0-intrinsic";
    private static final double W_ENGAGEMENT = 0.35;
    private static final double W_FOLLOWERS = 0.25;
    private static final double W_POST_VOLUME = 0.20;
    private static final double W_SENTIMENT = 0.15;
    private static final double W_AVAILABILITY = 0.05;

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

    @Enumerated(EnumType.STRING)
    private AgeGroup ageGroup;

    @Enumerated(EnumType.STRING)
    private GenderGroup gender;

    @Enumerated(EnumType.STRING)
    private GenderGroup genderTarget;

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
        double engagementComponent = normalizeEngagementRate();
        double followersComponent = normalizeFollowersByType();

        int postCount = 0;
        if (socialMediaAccounts != null) {
            for (SocialMedia sm : socialMediaAccounts) {
                if (sm != null && sm.getPosts() != null) {
                    postCount += sm.getPosts().size();
                }
            }
        }
        double postVolumeComponent = normalizePostVolume(postCount);

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
        double sentimentComponent = 0.0;
        if (sentimentCount > 0) {
            double avgSentiment = sentimentSum / sentimentCount;
            sentimentComponent = normalizeSentiment(avgSentiment);
            this.avgPostSentiment = Math.round(avgSentiment * 10000.0) / 10000.0; // keep a small scale
        } else {
            this.avgPostSentiment = null;
        }

        double availabilityComponent = Boolean.TRUE.equals(this.isAvailable) ? 1.0 : 0.0;

        double normalizedScore = engagementComponent * W_ENGAGEMENT
                + followersComponent * W_FOLLOWERS
                + postVolumeComponent * W_POST_VOLUME
                + sentimentComponent * W_SENTIMENT
                + availabilityComponent * W_AVAILABILITY;

        double score = Math.max(0.0, Math.min(1.0, normalizedScore)) * 100.0;

        this.influencerScore = Math.round(score * 100.0) / 100.0;
    }

    private double normalizeEngagementRate() {
        if (this.engagementRate == null) {
            return 0.0;
        }
        double engVal = Math.min(this.engagementRate.doubleValue(), 20.0);
        return Math.max(0.0, engVal / 20.0);
    }

    private double normalizeFollowersByType() {
        int followers = this.totalFollowers != null ? this.totalFollowers : 0;
        if (followers <= 0) {
            return 0.0;
        }

        int typeMaxFollowers = switch (this.influencerType) {
            case NANO -> 10_000;
            case MICRO -> 100_000;
            case MID_TIER -> 500_000;
            case MACRO -> 1_000_000;
            case MEGA -> 5_000_000;
            default -> 1_000_000;
        };

        double ratio = (double) followers / typeMaxFollowers;
        return Math.max(0.0, Math.min(1.0, ratio));
    }

    private double normalizePostVolume(int postCount) {
        if (postCount <= 0) {
            return 0.0;
        }
        return Math.min(postCount, 100.0) / 100.0;
    }

    private double normalizeSentiment(double avgSentiment) {
        return Math.max(0.0, Math.min(1.0, (avgSentiment + 1.0) / 2.0));
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

package org.ilias.influapp.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.ilias.influapp.entities.Enums.Category;
import org.ilias.influapp.entities.Enums.InfluencerType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

    public void removeSocialMediaAccount(SocialMedia account) {
        if (account == null) {
            return;
        }
        socialMediaAccounts.remove(account);
        account.setInfluencer(null);
    }

    public void addCollaboration(Collaboration collaboration) {
        if (collaboration == null) {
            return;
        }
        collaborations.add(collaboration);
        collaboration.setInfluencer(this);
    }

    public void removeCollaboration(Collaboration collaboration) {
        if (collaboration == null) {
            return;
        }
        collaborations.remove(collaboration);
        collaboration.setInfluencer(null);
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

        // Engagement component 40%
        if (this.engagementRate != null) {
            double engVal = Math.min(this.engagementRate.doubleValue(), 20.0);
            score += (engVal / 20.0) * 40.0;
        }

        //Followers component 30%
        int followers = this.totalFollowers != null ? this.totalFollowers : 0;
        if (followers > 0) {
            double logFollowers = Math.log10(followers);
            double logMax = Math.log10(1_000_000); // 6.0
            score += (Math.min(logFollowers, logMax) / logMax) * 30.0;
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

        // Availability bonus 10%
        if (Boolean.TRUE.equals(this.isAvailable)) {
            score += 10.0;
        }

        this.influencerScore = Math.round(score * 100.0) / 100.0;
    }
}

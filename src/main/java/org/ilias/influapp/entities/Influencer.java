package org.ilias.influapp.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

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

    private Double engagementRate;

    private Double influencerScore;

    @OneToMany(mappedBy = "influencer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SocialMedia> socialMediaAccounts = new ArrayList<>();

    @OneToMany(mappedBy = "influencer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Collaboration> collaborations = new ArrayList<>();

    public void addSocialMediaAccount(SocialMedia account) {
        socialMediaAccounts.add(account);
        account.setInfluencer(this);
        updateTotalFollowers();
    }

    public void addCollaboration(Collaboration collaboration) {
        collaborations.add(collaboration);
        collaboration.setInfluencer(this);
    }

    // TODO
    public void updateEngagementRate() {
        if (socialMediaAccounts != null && !socialMediaAccounts.isEmpty()) {
            this.engagementRate = socialMediaAccounts.stream()
                    .mapToDouble(SocialMedia::getEngagementRate)
                    .average()
                    .orElse(0.0);
        } else {
            this.engagementRate = 0.0;
        }
    }

    public void updateTotalFollowers() {
        if (socialMediaAccounts != null && !socialMediaAccounts.isEmpty()) {
            this.totalFollowers = socialMediaAccounts.stream()
                    .mapToInt(sm -> sm.getFollowers() == null ? 0 : sm.getFollowers())
                    .sum();
        } else {
            this.totalFollowers = 0;
        }
    }
}

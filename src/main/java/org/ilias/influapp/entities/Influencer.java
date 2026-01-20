package org.ilias.influapp.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "influencers")
@Getter
@Setter
public class Influencer extends User {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InfluencerType influencerType;

    private Integer totalFollowers;

    private Double engagementRate;

    @OneToMany(mappedBy = "influencer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SocialMedia> socialMediaAccounts;

    @OneToMany(mappedBy = "influencer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Collaboration> collaborations;

    public void addSocialMediaAccount(SocialMedia account) {
        socialMediaAccounts.add(account);
        account.setInfluencer(this);
        updateTotalFollowers();
    }

    public void addCollaboration(Collaboration collaboration) {
        collaborations.add(collaboration);
        collaboration.setInfluencer(this);
    }


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
                    .mapToInt(SocialMedia::getFollowers)
                    .sum();
        } else {
            this.totalFollowers = 0;
        }
    }
}

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
    }

    public void addCollaboration(Collaboration collaboration) {
    }


    public void updateEngagementRate() {
    }

    public void updateTotalFollowers() {

    }
}

package org.ilias.influapp.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.ilias.influapp.entities.Enums.Category;
import org.ilias.influapp.entities.Enums.CompanySize;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "businesses")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Business extends User {

    private String companyName;

    @Column(length = 2000)
    private String description;

    private String webSite;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Enumerated(EnumType.STRING)
    private CompanySize companySize;

    private String establishedYear;

    @Column(length = 1000)
    private String address;

    private String phone;

    private String contactEmail;

    private String imageUrl;

    private String linkedinUrl;
    private String facebookUrl;
    private String instagramUrl;
    private String twitterUrl;

    @Builder.Default
    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Campaign> campaigns = new ArrayList<>();

    public void addCampaign(Campaign campaign) {
        if (campaign == null) {
            return;
        }
        campaigns.add(campaign);
        campaign.setBusiness(this);
    }

    public void removeCampaign(Campaign campaign) {
        if (campaign == null) {
            return;
        }
        campaigns.remove(campaign);
        campaign.setBusiness(null);
    }

    public int getCampaignsSize() {
        return campaigns != null ? campaigns.size() : 0;
    }

    public double getTotalBudget() {
        if (campaigns == null || campaigns.isEmpty()) {
            return 0.0;
        }
        return campaigns.stream()
                .filter(campaign -> campaign != null && campaign.getBudget() != null)
                .mapToDouble(Campaign::getBudget)
                .sum();
    }
}
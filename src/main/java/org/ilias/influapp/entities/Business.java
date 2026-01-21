package org.ilias.influapp.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

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

    @Column(length = 1000)
    private String address;

    private String phone;

    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Campaign> campaigns = new ArrayList<>();

    public void addCampaign(Campaign campaign) {
        campaigns.add(campaign);
        campaign.setBusiness(this);
    }

    public void removeCampaign(Campaign campaign) {
        campaigns.remove(campaign);
        campaign.setBusiness(null);
    }

    public int getCampaignsSize() {
        return campaigns.size();
    }

    public double getTotalBudget() {
        return campaigns.stream()
                .mapToDouble(Campaign::getBudget)
                .sum();
    }
}
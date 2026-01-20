package org.ilias.influapp.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "businesses")
public class Business extends User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String companyName;

    @Column(length = 2000)
    private String description;

    private String webSite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
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
package org.ilias.influapp.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "campaigns")
@Getter
@Setter
public class Campaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category targetCategory;

    @Column(nullable = false)
    private Double budget;

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(length = 1000)
    private String goals;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Collaboration> collaborations = new ArrayList<>();

    public void addCollaboration(Collaboration collaboration) {
    }

    public  void removeCollaboration(Collaboration collaboration) {
    }

    public void isActive() {
    }

    public void getRemainingBudget() {
    }
}

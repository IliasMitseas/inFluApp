package org.ilias.influapp.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "collaborations")
@Getter
@Setter
public class Collaboration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "influencer_id", nullable = false)
    private Influencer influencer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CollaborationStatus status = CollaborationStatus.PENDING;

    @Column(nullable = false)
    private Double paymentAmount;

    @Column(length = 1000)
    private String deliverables;

    private LocalDate startDate;

    private LocalDate endDate;

    @OneToMany(mappedBy = "collaboration", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts = new ArrayList<>();

    @Column(length = 2000)
    private String notes;

    public boolean isActive() {
        return status == CollaborationStatus.ACCEPTED ||
                status == CollaborationStatus.IN_PROGRESS;
    }

    public boolean isCompleted() {
        return status == CollaborationStatus.COMPLETED;
    }
}

package org.ilias.influapp.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "social_media")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialMedia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "influencer_id", nullable = false)
    private Influencer influencer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    @Column(name = "account_url", nullable = false)
    private String accountUrl;

    private Integer followers;

    private Double engagementRate;
}

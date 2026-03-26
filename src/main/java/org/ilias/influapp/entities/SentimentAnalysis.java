package org.ilias.influapp.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sentiment_analysis")
public class SentimentAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, unique = true)
    private Post post;

    @Column(nullable = false)
    private Double polarity;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private Double confidence;

    @Column(nullable = false)
    private String modelVersion;

    @Column(nullable = false)
    private String method;

    @Column(nullable = false)
    private LocalDateTime analyzedAt;

    private Double coreNlpScore;

    private Double emojiScore;

    private Double reactionScore;
    
    @Column(name = "manual_label")
    private String manualLabel;
}


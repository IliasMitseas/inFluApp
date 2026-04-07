package org.ilias.influapp.entities;

import jakarta.persistence.*;
import lombok.*;
import org.ilias.influapp.entities.Enums.PostSentiment;

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

    // Backward-compatible human-readable label (e.g. "Positive", "Neutral")
    @Column(nullable = false)
    private String label;

    // Typed label used by scoring/ranking logic
    @Enumerated(EnumType.STRING)
    private PostSentiment sentimentLabel;

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

    // Provenance / audit fields for experiments
    private String language;
    private String source;

    @Column(length = 1000)
    private String notes;

    @Column(name = "manual_label")
    private String manualLabel;
}

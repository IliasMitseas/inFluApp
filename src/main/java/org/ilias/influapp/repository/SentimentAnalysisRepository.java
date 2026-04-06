package org.ilias.influapp.repository;

import org.ilias.influapp.entities.SentimentAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface SentimentAnalysisRepository extends JpaRepository<SentimentAnalysis, Long> {

    Optional<SentimentAnalysis> findByPostId(Long postId);

    @Query("SELECT AVG(sa.polarity) FROM SentimentAnalysis sa WHERE sa.post.socialMedia.influencer.id = :influencerId AND LOWER(sa.post.content) LIKE %:keyword%")
    Double avgPolarityByInfluencerIdAndKeyword(@Param("influencerId") Long influencerId, @Param("keyword") String keyword);
}


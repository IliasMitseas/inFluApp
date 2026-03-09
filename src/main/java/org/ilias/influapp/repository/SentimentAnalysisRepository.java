package org.ilias.influapp.repository;

import org.ilias.influapp.entities.SentimentAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SentimentAnalysisRepository extends JpaRepository<SentimentAnalysis, Long> {

    Optional<SentimentAnalysis> findByPostId(Long postId);
}


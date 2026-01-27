package org.ilias.influapp.repository;

import org.ilias.influapp.entities.Platform;
import org.ilias.influapp.entities.SocialMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SocialMediaRepository extends JpaRepository<SocialMedia, Long> {

    Optional<SocialMedia> findByInfluencerIdAndPlatform(Long influencerId, Platform platform);
    boolean existsByInfluencerIdAndPlatform(Long influencerId, Platform platform);
}

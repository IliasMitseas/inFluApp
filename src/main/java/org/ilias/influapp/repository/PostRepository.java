package org.ilias.influapp.repository;

import org.ilias.influapp.entities.Post;
import org.ilias.influapp.dtos.PlatformCountDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post,Long> {
    List<Post> findBySocialMediaId(Long socialMediaId);

    long countBySocialMediaInfluencerId(Long influencerId);

    List<Post> findTop5BySocialMediaInfluencerIdOrderByIdDesc(Long influencerId);

    // Average engagement rate for influencer's posts
    @Query("SELECT AVG(p.engagementRate) FROM posts p WHERE p.socialMedia.influencer.id = :influencerId AND p.engagementRate IS NOT NULL")
    Double avgEngagementRateByInfluencerId(@Param("influencerId") Long influencerId);

    // Count posts grouped by platform for an influencer
    @Query("SELECT new org.ilias.influapp.dtos.PlatformCountDto(p.socialMedia.platform, COUNT(p)) FROM posts p WHERE p.socialMedia.influencer.id = :influencerId GROUP BY p.socialMedia.platform")
    List<PlatformCountDto> countByPlatformForInfluencer(@Param("influencerId") Long influencerId);

    List<Post> findTop5BySocialMediaInfluencerIdOrderByEngagementRateDesc(Long influencerId);

    // Count posts for an influencer that contain a keyword (case-insensitive)
    long countBySocialMediaInfluencerIdAndContentContainingIgnoreCase(Long influencerId, String content);
}

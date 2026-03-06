package org.ilias.influapp.repository;

import org.ilias.influapp.entities.Collaboration;
import org.ilias.influapp.entities.Enums.CollaborationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollaborationRepository extends JpaRepository<Collaboration, Long> {

    List<Collaboration> findByInfluencerIdAndStatus(Long influencerId, CollaborationStatus status);

    List<Collaboration> findByInfluencerIdAndStatusIn(Long influencerId, List<CollaborationStatus> statuses);

    List<Collaboration> findByCampaignBusinessId(Long businessId);

    long countByInfluencerId(Long influencerId);

    @Query("SELECT COUNT(c) FROM Collaboration c WHERE c.campaign.business.id = :businessId")
    long countByCampaignBusinessId(@Param("businessId") Long businessId);

    @Query("SELECT SUM(c.paymentAmount) FROM Collaboration c WHERE c.campaign.business.id = :businessId")
    Double sumPaymentByBusinessId(@Param("businessId") Long businessId);
}

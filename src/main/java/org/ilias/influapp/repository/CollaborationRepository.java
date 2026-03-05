package org.ilias.influapp.repository;

import org.ilias.influapp.entities.Collaboration;
import org.ilias.influapp.entities.Enums.CollaborationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollaborationRepository extends JpaRepository<Collaboration, Long> {

    List<Collaboration> findByInfluencerIdAndStatus(Long influencerId, CollaborationStatus status);

    List<Collaboration> findByInfluencerIdAndStatusIn(Long influencerId, List<CollaborationStatus> statuses);
}

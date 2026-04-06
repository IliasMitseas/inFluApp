package org.ilias.influapp.repository;

import org.ilias.influapp.entities.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByBusinessId(Long businessId);
    Campaign findByTitle(String title);
}


package org.ilias.influapp.repository;

import org.ilias.influapp.entities.Category;
import org.ilias.influapp.entities.Influencer;
import org.ilias.influapp.entities.InfluencerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InfluencerRepository extends JpaRepository<Influencer, Long> {


    @Query("""
            select i
            from Influencer i
            where (:category is null or i.category = :category)
              and (:type is null or i.influencerType = :type)
              and (coalesce(i.influencerScore, 0) >= :minScore)
            order by coalesce(i.influencerScore, 0) desc
            """)
    List<Influencer> search(@Param("category") Category category,
                            @Param("type") InfluencerType type,
                            @Param("minScore") double minScore);
}

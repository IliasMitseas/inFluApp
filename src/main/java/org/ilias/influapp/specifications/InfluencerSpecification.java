package org.ilias.influapp.specifications;

import jakarta.persistence.criteria.Predicate;
import org.ilias.influapp.dtos.SearchDto;
import org.ilias.influapp.entities.Influencer;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.List;

public class InfluencerSpecification {
    public static Specification<Influencer> from(SearchDto searchDto) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (searchDto.getKeyword() != null && !searchDto.getKeyword().isEmpty()) {

                String pattern = "%" + searchDto.getKeyword().toLowerCase() + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("bio")), pattern)));
            }

            if (searchDto.getCategory() != null) {
                predicates.add(cb.equal(root.get("category"), searchDto.getCategory()));
            }

            if (searchDto.getType() != null) {
                predicates.add(cb.equal(root.get("influencerType"), searchDto.getType()));
            }

            if (searchDto.getMinFollowers() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalFollowers"), searchDto.getMinFollowers()));
            }

            if (searchDto.getMaxBudget() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("minCollaborationBudget"), searchDto.getMaxBudget()));
            }

            if (searchDto.getIsAvailable() != null) {
                predicates.add(cb.equal(root.get("isAvailable"), searchDto.getIsAvailable()));
            }

            if (searchDto.getLocation() != null && !searchDto.getLocation().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("location")), "%" + searchDto.getLocation().toLowerCase() + "%"));
            }

            // Engagement rate filters (engagementRate stored as BigDecimal on Influencer)
            if (searchDto.getMinEngagement() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("engagementRate"), BigDecimal.valueOf(searchDto.getMinEngagement())));
            }
            if (searchDto.getMaxEngagement() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("engagementRate"), BigDecimal.valueOf(searchDto.getMaxEngagement())));
            }

            // Overall sentiment filters (avgPostSentiment added to Influencer as Double)
            if (searchDto.getMinSentiment() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("avgPostSentiment"), searchDto.getMinSentiment()));
            }
            if (searchDto.getMaxSentiment() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("avgPostSentiment"), searchDto.getMaxSentiment()));
            }

            // Selected textual sentiment filter
            if (searchDto.getSelectedSentiment() != null) {
                switch (searchDto.getSelectedSentiment()) {
                    case LOVE -> predicates.add(cb.greaterThanOrEqualTo(root.get("avgPostSentiment"), 0.5));
                    case LIKE -> predicates.add(cb.and(cb.greaterThanOrEqualTo(root.get("avgPostSentiment"), 0.2), cb.lessThan(root.get("avgPostSentiment"), 0.5)));
                    case NEUTRAL -> predicates.add(cb.and(cb.greaterThanOrEqualTo(root.get("avgPostSentiment"), -0.2), cb.lessThan(root.get("avgPostSentiment"), 0.2)));
                    case DISLIKE -> predicates.add(cb.and(cb.greaterThanOrEqualTo(root.get("avgPostSentiment"), -0.5), cb.lessThan(root.get("avgPostSentiment"), -0.2)));
                    case TERRIBLE -> predicates.add(cb.lessThanOrEqualTo(root.get("avgPostSentiment"), -0.5));
                }
            }

            // Influencer score filters (influencerScore is Double)
            if (searchDto.getMinScore() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("influencerScore"), searchDto.getMinScore()));
            }
            if (searchDto.getMaxScore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("influencerScore"), searchDto.getMaxScore()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

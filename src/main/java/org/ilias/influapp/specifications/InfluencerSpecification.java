package org.ilias.influapp.specifications;

import jakarta.persistence.criteria.Predicate;
import org.ilias.influapp.dtos.SearchDto;
import org.ilias.influapp.entities.Influencer;
import org.springframework.data.jpa.domain.Specification;

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

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

package org.ilias.influapp.controllers;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.dtos.SearchDto;
import org.ilias.influapp.entities.Enums.Category;
import org.ilias.influapp.entities.Enums.InfluencerType;
import org.ilias.influapp.entities.Influencer;
import org.ilias.influapp.repository.InfluencerRepository;
import org.ilias.influapp.specifications.InfluencerSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class InfluencerSearchController {
    private final InfluencerRepository influencerRepository;
    private final org.ilias.influapp.services.RecommendationService recommendationService;
    @GetMapping("/influencers/search")
    public String search(@ModelAttribute("searchDto") SearchDto searchDto,
                         @RequestParam(required = false, value = "page", defaultValue = "0") int page,
                         @RequestParam(required = false, value = "size", defaultValue = "10") int size,
                         @RequestParam(required = false, value = "sort", defaultValue = "totalFollowers") String sortField,
                         @RequestParam(required = false, value = "dir", defaultValue = "DESC") String dir,
                         Model model) {

        if (page < 0) {
            page = 0;
        }
        if (size < 1) {
            size = 10;
        }
        if (size > 100) {
            size = 100;
        }

        Sort.Direction direction = Sort.Direction.fromString(dir.equalsIgnoreCase("asc") ? "ASC" : "DESC");
        Sort sortObj = Sort.by(direction, sortField);

        PageRequest pageable = PageRequest.of(page, size, sortObj);
        var spec = InfluencerSpecification.from(searchDto);

        Page<Influencer> results = influencerRepository.findAll(spec, pageable);

        model.addAttribute("results", results);
        model.addAttribute("currentPage", results.getNumber());
        model.addAttribute("size", results.getSize());
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", dir);
        model.addAttribute("totalPages", results.getTotalPages());
        model.addAttribute("allCategories", Category.values());
        model.addAttribute("allTypes", InfluencerType.values());

        return "influencer-search";
    }

    @GetMapping("/influencers/recommend-form")
    public String recommendForm(@ModelAttribute("searchDto") SearchDto searchDto,
                                @RequestParam(required = false, value = "businessId") Long businessId,
                                @RequestParam(required = false, value = "campaignId") Long campaignId,
                                @RequestParam(required = false, value = "keyword") String keyword,
                                @RequestParam(required = false, value = "location") String location,
                                @RequestParam(required = false, value = "ageGroup") org.ilias.influapp.entities.Enums.AgeGroup ageGroup,
                                @RequestParam(required = false, value = "genderTarget") org.ilias.influapp.entities.Enums.GenderGroup genderTarget,
                                @RequestParam(required = false, value = "limit", defaultValue = "5") int limit,
                                Model model) {
        // provide enums if template needs them
        model.addAttribute("allCategories", Category.values());
        model.addAttribute("allTypes", InfluencerType.values());

        // If the form provided any recommendation parameters, run the recommender and render results below the form
        boolean hasParams = (keyword != null && !keyword.isBlank()) || (location != null && !location.isBlank()) ||
                           ageGroup != null || genderTarget != null || limit > 0 || businessId != null || campaignId != null;
        if (hasParams) {
            var recs = businessId != null
                ? recommendationService.recommendForBusiness(businessId, campaignId, keyword, location, ageGroup, genderTarget, limit)
                : recommendationService.recommend(keyword, location, ageGroup, genderTarget, limit);
            model.addAttribute("recommendations", recs);
            model.addAttribute("recommendationCount", recs != null ? recs.size() : 0);
        }

        return "influencer-recommend-form";
    }




    @GetMapping("/influencers/recommend")
    public String recommend(@ModelAttribute("searchDto") SearchDto searchDto,
                            @RequestParam(required = false, value = "keyword") String keyword,
                            @RequestParam(required = false, value = "location") String location,
                            @RequestParam(required = false, value = "ageGroup") org.ilias.influapp.entities.Enums.AgeGroup ageGroup,
                            @RequestParam(required = false, value = "genderTarget") org.ilias.influapp.entities.Enums.GenderGroup genderTarget,
                            @RequestParam(required = false, value = "limit", defaultValue = "5") int limit,
                            Model model) {

        var recs = recommendationService.recommend(keyword, location, ageGroup, genderTarget, limit);
        model.addAttribute("recommendations", recs);
        model.addAttribute("searchDto", searchDto);
        model.addAttribute("currentPage", 0);
        model.addAttribute("size", 10);
        model.addAttribute("sortField", "totalFollowers");
        model.addAttribute("sortDir", "DESC");
        model.addAttribute("totalPages", 0);

        // keep search context and lists
        model.addAttribute("allCategories", Category.values());
        model.addAttribute("allTypes", InfluencerType.values());

        // render dedicated recommend results view
        return "influencer-search";
    }
}

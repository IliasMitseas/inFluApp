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
}

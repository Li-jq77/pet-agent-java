package com.ljq.petagent.controller;

import com.ljq.petagent.entity.Dog;
import com.ljq.petagent.entity.PetCategory;
import com.ljq.petagent.repository.DogRepository;
import com.ljq.petagent.repository.PetCategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MainController {

    private static final List<Long> SHUFFLED_DOG_IDS = Arrays.asList(
        13L, 9L, 14L, 19L, 18L, 2L, 10L, 7L, 8L, 17L,
        15L, 12L, 11L, 5L, 1L, 16L, 4L, 6L, 20L, 3L
    );
    private static final List<Long> SHUFFLED_DOG_POPULAR_IDS = Arrays.asList(
        4L, 16L, 12L, 17L, 13L, 1L, 10L, 19L, 14L, 7L, 3L, 20L, 5L, 2L
    );
    private static final List<Long> SHUFFLED_CAT_POPULAR_IDS = Arrays.asList(
        18L, 11L, 8L, 6L, 9L, 15L
    );
    private static final List<Long> SHUFFLED_POPULAR_IDS = Arrays.asList(
        12L, 18L, 8L, 6L, 16L, 17L, 11L, 4L
    );

    private final DogRepository dogRepository;
    private final PetCategoryRepository petCategoryRepository;

    public MainController(DogRepository dogRepository, PetCategoryRepository petCategoryRepository) {
        this.dogRepository = dogRepository;
        this.petCategoryRepository = petCategoryRepository;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<Dog> dogs = orderDogs(dogRepository.findByPublishedTrueOrderByIdAsc(), SHUFFLED_DOG_IDS);
        List<Dog> popularDogs = orderDogs(
            dogRepository.findByPublishedTrueOrderByIdAsc(),
            SHUFFLED_POPULAR_IDS
        );
        model.addAttribute("dogs", dogs);
        model.addAttribute("popularDogs", popularDogs);
        List<com.ljq.petagent.entity.PetCategory> dogCategories =
            petCategoryRepository.findByNameContainingOrderBySortOrderAsc("犬");
        List<com.ljq.petagent.entity.PetCategory> catCategories =
            petCategoryRepository.findByNameContainingOrderBySortOrderAsc("猫");
        java.util.Collections.reverse(dogCategories);
        java.util.Collections.reverse(catCategories);
        model.addAttribute("dogCategoriesReversed", dogCategories);
        model.addAttribute("catCategoriesReversed", catCategories);
        model.addAttribute("activeNav", "index");
        return "pets/index";
    }

    @GetMapping("/category/{slug}")
    public String category(
        @PathVariable String slug,
        @RequestParam(defaultValue = "1") int page,
        Model model
    ) {
        PetCategory category = petCategoryRepository.findBySlug(slug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Page<Dog> dogPage = dogRepository.findByPublishedTrueAndCategoryOrderByPopularDescCreatedAtDesc(
            category,
            PageRequest.of(Math.max(0, page - 1), 12)
        );
        model.addAttribute("category", category);
        model.addAttribute("dogs", dogPage.getContent());
        model.addAttribute("dogPage", dogPage);
        return "pets/category";
    }

    @GetMapping("/dog/{id}")
    public String dogDetail(@PathVariable Long id, Model model) {
        Dog dog = dogRepository.findByIdAndPublishedTrue(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<Dog> related = dogRepository
            .findByPublishedTrueAndCategoryAndIdNotOrderByPopularDescCreatedAtDesc(dog.getCategory(), dog.getId());
        model.addAttribute("dog", dog);
        model.addAttribute("relatedDogs", related.size() > 4 ? related.subList(0, 4) : related);
        return "pets/dog_detail";
    }

    private List<Dog> orderDogs(List<Dog> source, List<Long> ids) {
        Map<Long, Dog> byId = new LinkedHashMap<Long, Dog>();
        for (Dog dog : source) {
            byId.put(dog.getId(), dog);
        }
        List<Dog> result = new ArrayList<Dog>();
        for (Long id : ids) {
            Dog dog = byId.remove(id);
            if (dog != null) {
                result.add(dog);
            }
        }
        result.addAll(byId.values());
        return result;
    }
}

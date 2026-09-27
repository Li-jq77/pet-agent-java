package com.ljq.petagent.controller;

import com.ljq.petagent.repository.PetCategoryRepository;
import com.ljq.petagent.repository.ProductCategoryRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class GlobalModelAdvice {

    private final PetCategoryRepository petCategoryRepository;
    private final ProductCategoryRepository productCategoryRepository;

    public GlobalModelAdvice(
        PetCategoryRepository petCategoryRepository,
        ProductCategoryRepository productCategoryRepository
    ) {
        this.petCategoryRepository = petCategoryRepository;
        this.productCategoryRepository = productCategoryRepository;
    }

    @ModelAttribute("categories")
    public List<?> categories() {
        return petCategoryRepository.findAllByOrderBySortOrderAsc();
    }

    @ModelAttribute("dogCategories")
    public List<?> dogCategories() {
        return petCategoryRepository.findByNameContainingOrderBySortOrderAsc("犬");
    }

    @ModelAttribute("catCategories")
    public List<?> catCategories() {
        return petCategoryRepository.findByNameContainingOrderBySortOrderAsc("猫");
    }

    @ModelAttribute("productCategories")
    public List<?> productCategories() {
        return productCategoryRepository.findAllByOrderBySortOrderAsc();
    }

    @ModelAttribute("isLoggedIn")
    public boolean isLoggedIn(Authentication authentication) {
        return authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);
    }

    @ModelAttribute("currentUsername")
    public String currentUsername(Authentication authentication) {
        return isLoggedIn(authentication) ? authentication.getName() : "";
    }
}

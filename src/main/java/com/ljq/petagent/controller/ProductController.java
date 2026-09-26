package com.ljq.petagent.controller;

import com.ljq.petagent.entity.Product;
import com.ljq.petagent.entity.ProductCategory;
import com.ljq.petagent.repository.ProductCategoryRepository;
import com.ljq.petagent.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;

    public ProductController(
        ProductRepository productRepository,
        ProductCategoryRepository categoryRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/products")
    public String list(Model model) {
        model.addAttribute("currentCategory", null);
        model.addAttribute("products", productRepository.findByPublishedTrueOrderByCreatedAtDesc());
        return "pets/product_list";
    }

    @GetMapping("/products/{key}")
    public String byKey(@PathVariable String key, Model model) {
        if (key.chars().allMatch(Character::isDigit)) {
            Product product = productRepository.findByIdAndPublishedTrue(Long.valueOf(key))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            List<Product> related = productRepository
                .findByPublishedTrueAndCategoryAndIdNotOrderByCreatedAtDesc(product.getCategory(), product.getId());
            model.addAttribute("product", product);
            model.addAttribute("relatedProducts", related.size() > 3 ? related.subList(0, 3) : related);
            return "pets/product_detail";
        }

        ProductCategory category = categoryRepository.findBySlug(key)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("currentCategory", category);
        model.addAttribute("products", productRepository
            .findByPublishedTrueAndCategoryOrderByCreatedAtDesc(category));
        return "pets/product_list";
    }
}

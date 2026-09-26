package com.ljq.petagent.repository;

import com.ljq.petagent.entity.Product;
import com.ljq.petagent.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByPublishedTrueOrderByCreatedAtDesc();

    List<Product> findByPublishedTrueAndCategoryOrderByCreatedAtDesc(ProductCategory category);

    List<Product> findByPublishedTrueAndCategoryAndIdNotOrderByCreatedAtDesc(ProductCategory category, Long id);

    Optional<Product> findByIdAndPublishedTrue(Long id);
}

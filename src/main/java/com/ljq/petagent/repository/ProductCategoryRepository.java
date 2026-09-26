package com.ljq.petagent.repository;

import com.ljq.petagent.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    List<ProductCategory> findAllByOrderBySortOrderAsc();

    Optional<ProductCategory> findBySlug(String slug);
}

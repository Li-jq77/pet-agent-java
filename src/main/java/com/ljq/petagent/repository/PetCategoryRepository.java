package com.ljq.petagent.repository;

import com.ljq.petagent.entity.PetCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetCategoryRepository extends JpaRepository<PetCategory, Long> {

    List<PetCategory> findAllByOrderBySortOrderAsc();

    List<PetCategory> findByNameContainingOrderBySortOrderAsc(String keyword);

    Optional<PetCategory> findBySlug(String slug);
}

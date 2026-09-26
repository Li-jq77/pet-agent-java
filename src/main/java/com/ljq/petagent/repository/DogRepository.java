package com.ljq.petagent.repository;

import com.ljq.petagent.entity.Dog;
import com.ljq.petagent.entity.PetCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DogRepository extends JpaRepository<Dog, Long> {

    List<Dog> findByPublishedTrueOrderByIdAsc();

    List<Dog> findByPublishedTrueAndPopularTrueAndCategorySlugInOrderByIdAsc(Collection<String> slugs);

    List<Dog> findByPublishedTrueAndPopularTrueAndCategorySlugNotInOrderByIdAsc(Collection<String> slugs);

    Page<Dog> findByPublishedTrueAndCategoryOrderByPopularDescCreatedAtDesc(PetCategory category, Pageable pageable);

    List<Dog> findByPublishedTrueAndCategoryAndIdNotOrderByPopularDescCreatedAtDesc(PetCategory category, Long id);

    Optional<Dog> findByIdAndPublishedTrue(Long id);
}

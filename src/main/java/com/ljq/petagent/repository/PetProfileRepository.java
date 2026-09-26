package com.ljq.petagent.repository;

import com.ljq.petagent.entity.PetProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetProfileRepository extends JpaRepository<PetProfile, Long> {

    List<PetProfile> findByOwnerIdAndActiveTrueOrderByCreatedAtDesc(Long ownerId);

    List<PetProfile> findByOwnerIdOrderByIdDesc(Long ownerId);

    long countByOwnerIdAndActiveTrue(Long ownerId);

    Optional<PetProfile> findByIdAndOwnerId(Long id, Long ownerId);
}

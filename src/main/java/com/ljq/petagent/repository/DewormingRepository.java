package com.ljq.petagent.repository;

import com.ljq.petagent.entity.Deworming;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DewormingRepository extends JpaRepository<Deworming, Long> {

    List<Deworming> findByPetIdOrderByAdministeredDateDesc(Long petId);
}

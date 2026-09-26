package com.ljq.petagent.repository;

import com.ljq.petagent.entity.Vaccination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VaccinationRepository extends JpaRepository<Vaccination, Long> {

    List<Vaccination> findByPetIdOrderByAdministeredDateDesc(Long petId);
}

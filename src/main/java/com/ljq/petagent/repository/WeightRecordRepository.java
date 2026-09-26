package com.ljq.petagent.repository;

import com.ljq.petagent.entity.WeightRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WeightRecordRepository extends JpaRepository<WeightRecord, Long> {

    List<WeightRecord> findByPetIdOrderByRecordedAtAsc(Long petId);

    Optional<WeightRecord> findFirstByPetIdOrderByRecordedAtDescCreatedAtDesc(Long petId);
}

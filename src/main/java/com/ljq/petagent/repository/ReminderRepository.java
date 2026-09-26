package com.ljq.petagent.repository;

import com.ljq.petagent.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByPetOwnerIdAndReminderDateBetweenAndCompletedFalseOrderByReminderDateAsc(
        Long ownerId,
        LocalDate start,
        LocalDate end
    );

    Optional<Reminder> findFirstByPetOwnerIdAndReminderTypeAndReminderDateGreaterThanEqualAndCompletedFalseOrderByReminderDateAsc(
        Long ownerId,
        String reminderType,
        LocalDate date
    );

    List<Reminder> findByPetIdAndCompletedFalseOrderByReminderDateAsc(Long petId);

    List<Reminder> findByPetOwnerIdAndReminderDateBetweenOrderByReminderDateAsc(
        Long ownerId,
        LocalDate start,
        LocalDate end
    );

    boolean existsByPetIdAndReminderTypeAndReminderDate(Long petId, String reminderType, LocalDate reminderDate);

    boolean existsByPetIdAndReminderType(Long petId, String reminderType);
}

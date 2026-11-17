package com.etoile.datesservice.date;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RescheduleRepository extends JpaRepository<Reschedule, UUID> {

    Optional<Reschedule> findByDatePlanIdAndStatus(UUID datePlanId, RescheduleStatus status);
}

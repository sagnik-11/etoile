package com.etoile.datesservice.date;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DatePlanRepository extends JpaRepository<DatePlan, UUID> {

    List<DatePlan> findByStatusInOrderByStartsAtAsc(Collection<DateStatus> statuses);

    List<DatePlan> findAllByOrderByStartsAtDesc();
}

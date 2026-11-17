package com.etoile.datesservice.date;

import com.etoile.datesservice.security.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MovieSuggestionRepository extends JpaRepository<MovieSuggestion, UUID> {

    List<MovieSuggestion> findByDatePlanIdOrderByCreatedAtAsc(UUID datePlanId);

    long countByDatePlanIdAndSuggestedByRole(UUID datePlanId, Role role);
}

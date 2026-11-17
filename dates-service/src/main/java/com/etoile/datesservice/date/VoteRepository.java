package com.etoile.datesservice.date;

import com.etoile.datesservice.security.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VoteRepository extends JpaRepository<Vote, UUID> {

    List<Vote> findByDatePlanId(UUID datePlanId);

    Optional<Vote> findByDatePlanIdAndVoterRole(UUID datePlanId, Role voterRole);

    boolean existsByDatePlanIdAndSuggestionId(UUID datePlanId, UUID suggestionId);

    long deleteByDatePlanId(UUID datePlanId);
}

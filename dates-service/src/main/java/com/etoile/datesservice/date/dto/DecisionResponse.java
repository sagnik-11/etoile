package com.etoile.datesservice.date.dto;

import com.etoile.datesservice.date.DecisionMethod;

import java.util.UUID;

public record DecisionResponse(
        UUID movieId,          // null while undecided
        String title,
        DecisionMethod method,
        boolean isFinal,       // true after the random pick
        UUID herPickId,        // the 2 narrowed movies after a failed vote
        UUID himPickId
) {
}

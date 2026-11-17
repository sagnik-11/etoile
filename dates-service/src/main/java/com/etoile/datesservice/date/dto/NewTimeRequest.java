package com.etoile.datesservice.date.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record NewTimeRequest(
        @NotNull @Future Instant startsAt,
        @Future Instant endsAt
) {
}

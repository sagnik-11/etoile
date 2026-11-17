package com.etoile.datesservice.date.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateTripDateRequest(
        @NotNull @Future Instant startsAt,
        @NotNull @Future Instant endsAt,
        @NotBlank @Size(max = 255) String place,
        @Size(max = 10000) String itinerary
) {
}

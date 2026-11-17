package com.etoile.datesservice.date.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record CreateMovieDateRequest(
        @NotNull @Future Instant startsAt,
        @Size(max = 2) List<@Valid SuggestionRequest> suggestions
) {
}

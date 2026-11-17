package com.etoile.datesservice.date.dto;

import jakarta.validation.Valid;

import java.util.UUID;

// Either an existing movie's id, or a brand-new movie - never both.
public record AgreeRequest(UUID suggestionId, @Valid SuggestionRequest newMovie) {
}

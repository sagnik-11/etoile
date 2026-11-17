package com.etoile.datesservice.date.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VoteRequest(@NotNull UUID suggestionId) {
}

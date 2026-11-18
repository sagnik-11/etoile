package com.etoile.notificationservice.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

// Sent by dates-service. startsAt is the date's (new) time; movieTitle/decisionMethod only for movie events.
public record DateEventRequest(
        @NotNull DateEventType type,
        @NotNull UUID dateId,
        @NotBlank @Size(max = 20) String dateType,
        @NotNull Instant startsAt,
        @Size(max = 200) String movieTitle,
        @Size(max = 30) String decisionMethod) {
}

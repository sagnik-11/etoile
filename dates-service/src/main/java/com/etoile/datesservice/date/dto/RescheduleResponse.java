package com.etoile.datesservice.date.dto;

import com.etoile.datesservice.date.Reschedule;
import com.etoile.datesservice.security.Role;

import java.time.Instant;
import java.util.UUID;

public record RescheduleResponse(UUID id, Role requestedBy, Instant requestedAt, Instant acknowledgedAt) {

    public static RescheduleResponse from(Reschedule r) {
        return r == null ? null : new RescheduleResponse(r.getId(), r.getRequestedBy(), r.getRequestedAt(), r.getAcknowledgedAt());
    }
}

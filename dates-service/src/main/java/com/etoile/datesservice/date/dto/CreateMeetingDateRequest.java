package com.etoile.datesservice.date.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateMeetingDateRequest(
        @NotNull @Future Instant startsAt,
        @NotBlank @Size(max = 255) String place,
        @Size(max = 1000) String mapsLink,
        @Size(max = 255) String activity
) {
}

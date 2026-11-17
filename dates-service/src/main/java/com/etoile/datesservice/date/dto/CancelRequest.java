package com.etoile.datesservice.date.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CancelRequest(
        @NotNull @Min(value = 5, message = "say sorry 5 times first") Integer sorryCount
) {
}

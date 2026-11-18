package com.etoile.notificationservice.push;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SeenRequest(@NotBlank @Size(max = 100) String groupKey) {
}

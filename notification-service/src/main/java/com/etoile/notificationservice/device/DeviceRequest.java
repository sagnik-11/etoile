package com.etoile.notificationservice.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceRequest(@NotBlank @Size(max = 512) String token) {
}

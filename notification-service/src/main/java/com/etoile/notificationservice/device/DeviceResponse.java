package com.etoile.notificationservice.device;

import com.etoile.notificationservice.security.Role;

public record DeviceResponse(Role role, long devices) {
}

package com.etoile.notificationservice.device;

import com.etoile.notificationservice.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    public DeviceResponse register(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DeviceRequest request) {
        return deviceService.register(CurrentUser.from(jwt), request.token());
    }

    @PostMapping("/unregister")
    public DeviceResponse unregister(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DeviceRequest request) {
        return deviceService.unregister(CurrentUser.from(jwt), request.token());
    }
}

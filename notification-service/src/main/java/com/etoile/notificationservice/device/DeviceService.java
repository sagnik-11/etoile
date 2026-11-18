package com.etoile.notificationservice.device;

import com.etoile.notificationservice.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceService {

    private final DeviceTokenRepository devices;

    public DeviceService(DeviceTokenRepository devices) {
        this.devices = devices;
    }

    @Transactional
    public DeviceResponse register(CurrentUser me, String token) {
        devices.findByToken(token)
                .ifPresentOrElse(
                        d -> d.assignTo(me.role(), me.id()),
                        () -> devices.save(new DeviceToken(token, me.role(), me.id())));
        return new DeviceResponse(me.role(), devices.countByRole(me.role()));
    }

    @Transactional
    public DeviceResponse unregister(CurrentUser me, String token) {
        devices.deleteByToken(token);
        return new DeviceResponse(me.role(), devices.countByRole(me.role()));
    }
}

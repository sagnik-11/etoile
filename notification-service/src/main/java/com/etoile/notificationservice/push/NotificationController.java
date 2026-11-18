package com.etoile.notificationservice.push;

import com.etoile.notificationservice.device.DeviceTokenRepository;
import com.etoile.notificationservice.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final PushSender sender;
    private final NotificationGroups groups;
    private final DeviceTokenRepository devices;

    public NotificationController(PushSender sender, NotificationGroups groups, DeviceTokenRepository devices) {
        this.sender = sender;
        this.groups = groups;
        this.devices = devices;
    }

    // The app calls this when you open a date or the love icon: resets that group's count.
    @PostMapping("/seen")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void seen(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SeenRequest request) {
        groups.seen(CurrentUser.from(jwt).role(), request.groupKey());
    }

    // Sends a test push to your own devices. Repeat it to see merging ("It works! 🔔 ×3").
    @PostMapping("/test")
    public TestPushResponse test(@AuthenticationPrincipal Jwt jwt) {
        CurrentUser me = CurrentUser.from(jwt);
        int count = groups.bump(me.role(), "test");
        String body = count == 1 ? "It works! 🔔" : "It works! 🔔 ×" + count;
        int deviceCount = devices.findByRole(me.role()).size();
        sender.send(me.role(), new PushMessage("test", "Étoile test push", body, Map.of("type", "TEST")));
        return new TestPushResponse(sender.isLive(), deviceCount, count);
    }
}

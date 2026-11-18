package com.etoile.notificationservice.event;

import com.etoile.notificationservice.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class DateEventController {

    private final DateEventService events;

    public DateEventController(DateEventService events) {
        this.events = events;
    }

    // Called by dates-service with the acting person's own login token.
    @PostMapping("/date")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dateEvent(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DateEventRequest request) {
        events.handle(CurrentUser.from(jwt), request);
    }
}

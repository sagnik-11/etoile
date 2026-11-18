package com.etoile.datesservice.notify;

import java.util.UUID;

// token = the acting person's own login token, forwarded so notification-service knows who did it.
public record DateEvent(String token, Body body) {

    public record Body(DateEventType type, UUID dateId, String dateType, String startsAt,
                       String movieTitle, String decisionMethod) {
    }
}

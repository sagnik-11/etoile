package com.etoile.datesservice.notify;

import com.etoile.datesservice.date.DatePlan;
import com.etoile.datesservice.date.DecisionMethod;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class DateEvents {

    private final ApplicationEventPublisher publisher;

    public DateEvents(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(DateEventType type, DatePlan date) {
        publish(type, date, null, null);
    }

    public void publish(DateEventType type, DatePlan date, String movieTitle, DecisionMethod method) {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken auth)) {
            return;   // no logged-in person (should not happen for our endpoints)
        }
        publisher.publishEvent(new DateEvent(auth.getToken().getTokenValue(), new DateEvent.Body(
                type, date.getId(), date.getType().name(), date.getStartsAt().toString(),
                movieTitle, method == null ? null : method.name())));
    }
}

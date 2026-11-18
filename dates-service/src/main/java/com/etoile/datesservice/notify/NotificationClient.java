package com.etoile.datesservice.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final RestClient http;

    public NotificationClient(@Value("${notifications.url}") String url) {
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofSeconds(10));
        timeouts.setReadTimeout(Duration.ofSeconds(60));   // a sleeping server can take a while to wake
        this.http = RestClient.builder().baseUrl(url).requestFactory(timeouts).build();
    }

    // Runs only after the database change is saved, on a background thread.
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void send(DateEvent e) {
        try {
            http.post().uri("/api/events/date")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + e.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(e.body())
                    .retrieve()
                    .toBodilessEntity();
            log.info("Notified {} for date {}", e.body().type(), e.body().dateId());
        } catch (Exception ex) {
            log.warn("Could not notify {} for date {}: {}", e.body().type(), e.body().dateId(), ex.getMessage());
        }
    }
}

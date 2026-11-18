package com.etoile.notificationservice.reminder;

import com.etoile.notificationservice.push.PushMessage;
import com.etoile.notificationservice.push.PushSender;
import com.etoile.notificationservice.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class Reminders {

    private static final Logger log = LoggerFactory.getLogger(Reminders.class);

    private final ReminderRepository reminders;
    private final PushSender sender;
    private final Duration every;
    private final Duration window;
    private final ZoneId zone;
    private final LocalTime quietStart;
    private final LocalTime quietEnd;

    public Reminders(ReminderRepository reminders, PushSender sender,
                     @Value("${notifications.reminder-every-seconds}") long everySeconds,
                     @Value("${notifications.reminder-window-seconds}") long windowSeconds,
                     @Value("${notifications.zone}") String zone,
                     @Value("${notifications.quiet-start}") String quietStart,
                     @Value("${notifications.quiet-end}") String quietEnd) {
        this.reminders = reminders;
        this.sender = sender;
        this.every = Duration.ofSeconds(everySeconds);
        this.window = Duration.ofSeconds(windowSeconds);
        this.zone = ZoneId.of(zone);
        this.quietStart = LocalTime.parse(quietStart);
        this.quietEnd = LocalTime.parse(quietEnd);
    }

    @Transactional
    public void start(ReminderKind kind, Role to, String refKey, String title, String body,
                      UUID dateId, Instant stopAt) {
        Reminder r = reminders.findByKindAndRecipientAndRefKey(kind, to, refKey)
                .orElseGet(() -> new Reminder(kind, to, refKey));   // not saved yet
        r.restart(title, body, dateId, stopAt);                      // fill everything in
        reminders.save(r);                                           // then save
    }

    @Transactional
    public void stop(ReminderKind kind, Role to, String refKey) {
        reminders.deleteByKindAndRecipientAndRefKey(kind, to, refKey);
    }

    @Transactional
    public void stopAll(ReminderKind kind, String refKey) {
        reminders.deleteByKindAndRefKey(kind, refKey);
    }

    // Runs on a timer. Sends at most one reminder per row per run.
    @Scheduled(fixedDelayString = "${notifications.reminder-tick-ms}")
    @Transactional
    public void tick() {
        Instant now = Instant.now();
        if (isQuiet(now)) {
            return;   // paused at night; quiet time doesn't count toward the window
        }
        int max = (int) (window.toSeconds() / every.toSeconds());
        for (Reminder r : reminders.findAll()) {
            boolean expired = r.getStopAt() != null && now.isAfter(r.getStopAt());
            if (expired || r.getSent() >= max) {
                reminders.delete(r);
                continue;
            }
            Duration active = activeTime(r.getStartedAt(), now);
            if (active.compareTo(every.multipliedBy(r.getSent() + 1)) >= 0) {
                Map<String, String> data = new HashMap<>();
                data.put("type", r.getKind() == ReminderKind.NEED_LOVE ? "NEED_LOVE" : "RESCHEDULE_REQUESTED");
                if (r.getDateId() != null) data.put("dateId", r.getDateId().toString());
                sender.send(r.getRecipient(), new PushMessage(r.getRefKey(), r.getTitle(), r.getBody(), data));
                int n = r.markSent();
                log.info("Reminder {}/{} for {} [{}]", n, max, r.getRecipient(), r.getRefKey());
            }
        }
    }

    boolean isQuiet(Instant t) {
        LocalTime time = t.atZone(zone).toLocalTime();
        return !time.isBefore(quietStart) && time.isBefore(quietEnd);
    }

    // Time between start and now, minus any quiet hours in between.
    Duration activeTime(Instant start, Instant now) {
        Duration total = Duration.between(start, now);
        LocalDate day = start.atZone(zone).toLocalDate();
        LocalDate last = now.atZone(zone).toLocalDate();
        while (!day.isAfter(last)) {
            Instant qs = day.atTime(quietStart).atZone(zone).toInstant();
            Instant qe = day.atTime(quietEnd).atZone(zone).toInstant();
            Instant from = qs.isAfter(start) ? qs : start;
            Instant to = qe.isBefore(now) ? qe : now;
            if (to.isAfter(from)) {
                total = total.minus(Duration.between(from, to));
            }
            day = day.plusDays(1);
        }
        return total;
    }
}

package com.etoile.notificationservice.event;

import com.etoile.notificationservice.push.NotificationGroups;
import com.etoile.notificationservice.push.PushMessage;
import com.etoile.notificationservice.push.PushSender;
import com.etoile.notificationservice.reminder.ReminderKind;
import com.etoile.notificationservice.reminder.Reminders;
import com.etoile.notificationservice.security.CurrentUser;
import com.etoile.notificationservice.security.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Service
public class DateEventService {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.ENGLISH);

    private final PushSender sender;
    private final NotificationGroups groups;
    private final ZoneId zone;
    private final Reminders reminders;

    public DateEventService(PushSender sender, NotificationGroups groups, Reminders reminders,
                            @Value("${notifications.zone}") String zone) {
        this.sender = sender;
        this.groups = groups;
        this.reminders = reminders;
        this.zone = ZoneId.of(zone);
    }

    public void handle(CurrentUser actor, DateEventRequest e) {
        Role other = actor.role().other();
        String key = "date-" + e.dateId();
        String who = actor.name();
        String kind = kind(e.dateType());
        String when = WHEN.format(e.startsAt().atZone(zone));
        String movie = e.movieTitle() == null ? "a movie" : "\"" + e.movieTitle() + "\"";

        PushMessage m = switch (e.type()) {
            case DATE_PROPOSED -> msg(key, e, "New date invite 💌", who + " invited you to a " + kind + " on " + when);
            case DATE_ACCEPTED -> msg(key, e, "It's a date! 🥰", who + " said yes to the " + kind + " on " + when);
            case DATE_REJECTED -> msg(key, e, "Date declined", who + " can't make the " + kind + " on " + when);
            case DATE_CANCELLED -> msg(key, e, "Date cancelled 💔", who + " cancelled the " + kind + " on " + when + " (after 5 sorrys)");
            case RESCHEDULE_REQUESTED -> msg(key, e, "We need to talk...", who + " wants to reschedule the " + kind + " on " + when);
            case RESCHEDULE_ACKNOWLEDGED -> msg(key, e, "Yeah, yeah... coming", who + " saw your reschedule request for the " + kind);
            case NEW_TIME_SET -> msg(key, e, "New time ⏰", who + " moved the " + kind + " to " + when);
            case RESCHEDULE_CANCELLED -> msg(key, e, "Never mind 😌", who + " cancelled the reschedule. The " + kind + " stays on " + when);
            case MOVIE_SUGGESTED -> {
                int n = groups.bump(other, key);
                yield msg(key, e, "New movie 🍿", n == 1
                        ? who + " suggested " + movie + " for the movie date on " + when
                        : n + " new movies for your movie date on " + when);
            }
            case VOTED -> msg(key, e, who + " has voted 🗳️", "Your turn to vote for the movie date on " + when);
            case VOTE_SPLIT -> msg(key, e, "Votes split 🤔", "You two picked different movies. Time for a call 📞");
            case MOVIE_DECIDED -> msg(key, e, "Movie decided 🎬", decided(e.decisionMethod(), movie));
        };

        sender.send(other, m);
        if (e.type() == DateEventType.MOVIE_DECIDED) {
            sender.send(actor.role(), m);   // both of you hear about the final movie
        }

        switch (e.type()) {
            case RESCHEDULE_REQUESTED -> reminders.start(ReminderKind.RESCHEDULE, other, key, "We need to talk...",
                    "Still waiting… " + who + " wants to reschedule the " + kind + " on " + when + " 👀",
                    e.dateId(), e.startsAt());
            case RESCHEDULE_ACKNOWLEDGED, NEW_TIME_SET, RESCHEDULE_CANCELLED, DATE_CANCELLED ->
                    reminders.stopAll(ReminderKind.RESCHEDULE, key);
            default -> { }
        }
    }

    private static String decided(String method, String movie) {
        if (method == null) return movie + " it is!";
        return switch (method) {
            case "VOTE" -> "You both picked " + movie + " 🎉";
            case "AGREED_NARROWED" -> "We talked, we agreed 🤝 " + movie;
            case "AGREED_OTHER" -> "Okay fine, this one 😤 " + movie;
            case "RANDOM" -> "The app has spoken 🎲 " + movie + ". It's final!";
            default -> movie + " it is!";
        };
    }

    private static String kind(String dateType) {
        return switch (dateType) {
            case "MOVIE" -> "movie date";
            case "MEETING" -> "meeting";
            case "TRIP" -> "trip";
            default -> "date";
        };
    }

    private static PushMessage msg(String key, DateEventRequest e, String title, String body) {
        return new PushMessage(key, title, body, Map.of("type", e.type().name(), "dateId", e.dateId().toString()));
    }
}

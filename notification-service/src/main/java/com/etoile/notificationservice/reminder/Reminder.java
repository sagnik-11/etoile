package com.etoile.notificationservice.reminder;

import com.etoile.notificationservice.security.Role;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reminders",
        uniqueConstraints = @UniqueConstraint(columnNames = {"kind", "recipient", "ref_key"}))
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReminderKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role recipient;

    @Column(name = "ref_key", nullable = false, length = 100)
    private String refKey;          // "love" or "date-<id>"; also the notification tag

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 300)
    private String body;

    @Column(name = "date_id")
    private UUID dateId;            // only for reschedule reminders

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "stop_at")
    private Instant stopAt;         // e.g. the date's start time

    @Column(nullable = false)
    private int sent;

    protected Reminder() {
    }

    public Reminder(ReminderKind kind, Role recipient, String refKey) {
        this.kind = kind;
        this.recipient = recipient;
        this.refKey = refKey;
    }

    // (Re)starts the window from now with fresh texts.
    public void restart(String title, String body, UUID dateId, Instant stopAt) {
        this.title = title;
        this.body = body;
        this.dateId = dateId;
        this.stopAt = stopAt;
        this.startedAt = Instant.now();
        this.sent = 0;
    }

    public int markSent() { return ++sent; }

    public ReminderKind getKind() { return kind; }
    public Role getRecipient() { return recipient; }
    public String getRefKey() { return refKey; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public UUID getDateId() { return dateId; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getStopAt() { return stopAt; }
    public int getSent() { return sent; }
}

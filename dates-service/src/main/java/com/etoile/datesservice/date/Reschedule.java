package com.etoile.datesservice.date;

import com.etoile.datesservice.security.Role;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reschedules")
public class Reschedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "date_plan_id", nullable = false)
    private DatePlan datePlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_by", nullable = false, length = 10)
    private Role requestedBy;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RescheduleStatus status;

    @Column(name = "old_starts_at", nullable = false)
    private Instant oldStartsAt;

    @Column(name = "new_starts_at")
    private Instant newStartsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "closed_by", length = 10)
    private Role closedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    protected Reschedule() {
    }

    public Reschedule(DatePlan datePlan, Role requestedBy) {
        this.datePlan = datePlan;
        this.requestedBy = requestedBy;
        this.requestedAt = Instant.now();
        this.status = RescheduleStatus.OPEN;
        this.oldStartsAt = datePlan.getStartsAt();
    }

    public void acknowledge() {
        if (acknowledgedAt == null) {
            acknowledgedAt = Instant.now();
        }
    }

    public void resolve(Role by, Instant newStartsAt) {
        this.status = RescheduleStatus.RESOLVED;
        this.newStartsAt = newStartsAt;
        close(by);
    }

    public void cancel(Role by) {
        this.status = RescheduleStatus.CANCELLED;
        close(by);
    }

    private void close(Role by) {
        this.closedBy = by;
        this.closedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Role getRequestedBy() { return requestedBy; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public RescheduleStatus getStatus() { return status; }
}

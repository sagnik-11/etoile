package com.etoile.datesservice.date;

import com.etoile.datesservice.security.CurrentUser;
import com.etoile.datesservice.security.Role;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "date_plans")
public class DatePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DateType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DateStatus status;

    @Column(name = "proposer_id", nullable = false)
    private UUID proposerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposer_role", nullable = false, length = 10)
    private Role proposerRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_by", length = 10)
    private Role cancelledBy;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    private String place;

    @Column(name = "maps_link", length = 1000)
    private String mapsLink;

    private String activity;

    @Column(columnDefinition = "text")
    private String itinerary;

    @ColumnDefault("false")
    @Column(name = "voting_started", nullable = false)
    private boolean votingStarted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_movie_id")
    private MovieSuggestion decidedMovie;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_method", length = 20)
    private DecisionMethod decisionMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "her_pick_id")
    private MovieSuggestion herPick;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "him_pick_id")
    private MovieSuggestion himPick;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DatePlan() {
    }

    private DatePlan(DateType type, CurrentUser proposer, Instant startsAt) {
        this.type = type;
        this.status = DateStatus.PROPOSED;
        this.proposerId = proposer.id();
        this.proposerRole = proposer.role();
        this.startsAt = startsAt;
    }

    public static DatePlan movie(CurrentUser proposer, Instant startsAt) {
        return new DatePlan(DateType.MOVIE, proposer, startsAt);
    }

    public static DatePlan meeting(CurrentUser proposer, Instant startsAt, String place, String mapsLink, String activity) {
        DatePlan d = new DatePlan(DateType.MEETING, proposer, startsAt);
        d.place = place;
        d.mapsLink = mapsLink;
        d.activity = activity;
        return d;
    }

    public static DatePlan trip(CurrentUser proposer, Instant startsAt, Instant endsAt, String place, String itinerary) {
        DatePlan d = new DatePlan(DateType.TRIP, proposer, startsAt);
        d.endsAt = endsAt;
        d.place = place;
        d.itinerary = itinerary;
        return d;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void accept() {
        this.status = DateStatus.ACCEPTED;
    }

    public void reject() {
        this.status = DateStatus.REJECTED;
    }

    public void moveTo(Instant startsAt, Instant endsAt) {
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public void cancel(Role by) {
        this.status = DateStatus.CANCELLED;
        this.cancelledBy = by;
    }

    public void startVoting() {
        this.votingStarted = true;
    }

    public void narrow(MovieSuggestion herPick, MovieSuggestion himPick) {
        this.herPick = herPick;
        this.himPick = himPick;
    }

    public void decide(MovieSuggestion movie, DecisionMethod method) {
        this.decidedMovie = movie;
        this.decisionMethod = method;
    }

    public void resetDecision() {
        this.decidedMovie = null;
        this.decisionMethod = null;
        this.herPick = null;
        this.himPick = null;
    }

    public boolean isLocked() { return decisionMethod == DecisionMethod.RANDOM; }

    public MovieSuggestion getDecidedMovie() { return decidedMovie; }
    public DecisionMethod getDecisionMethod() { return decisionMethod; }
    public MovieSuggestion getHerPick() { return herPick; }
    public MovieSuggestion getHimPick() { return himPick; }

    public UUID getId() { return id; }
    public DateType getType() { return type; }
    public DateStatus getStatus() { return status; }
    public UUID getProposerId() { return proposerId; }
    public Role getProposerRole() { return proposerRole; }
    public Role getCancelledBy() { return cancelledBy; }
    public boolean isVotingStarted() { return votingStarted; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public String getPlace() { return place; }
    public String getMapsLink() { return mapsLink; }
    public String getActivity() { return activity; }
    public String getItinerary() { return itinerary; }
    public Instant getCreatedAt() { return createdAt; }
}

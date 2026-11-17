package com.etoile.datesservice.date;

import com.etoile.datesservice.security.Role;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "votes", uniqueConstraints = @UniqueConstraint(columnNames = {"date_plan_id", "voter_role"}))
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "date_plan_id", nullable = false)
    private DatePlan datePlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "voter_role", nullable = false, length = 10)
    private Role voterRole;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "suggestion_id", nullable = false)
    private MovieSuggestion suggestion;

    protected Vote() {
    }

    public Vote(DatePlan datePlan, Role voterRole, MovieSuggestion suggestion) {
        this.datePlan = datePlan;
        this.voterRole = voterRole;
        this.suggestion = suggestion;
    }

    public void changeTo(MovieSuggestion suggestion) {
        this.suggestion = suggestion;
    }

    public Role getVoterRole() { return voterRole; }
    public MovieSuggestion getSuggestion() { return suggestion; }
}

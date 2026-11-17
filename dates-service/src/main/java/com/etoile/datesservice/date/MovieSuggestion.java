package com.etoile.datesservice.date;

import com.etoile.datesservice.security.CurrentUser;
import com.etoile.datesservice.security.Role;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "movie_suggestions")
public class MovieSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "date_plan_id", nullable = false)
    private DatePlan datePlan;

    @Column(name = "suggested_by_id", nullable = false)
    private UUID suggestedById;

    @Enumerated(EnumType.STRING)
    @Column(name = "suggested_by_role", nullable = false, length = 10)
    private Role suggestedByRole;

    @Column(nullable = false)
    private String title;

    @Column(name = "release_year", nullable = false)
    private int releaseYear;

    @Column(columnDefinition = "text")
    private String synopsis;

    @Column(name = "youtube_url", length = 1000)
    private String youtubeUrl;

    @ElementCollection
    @CollectionTable(name = "movie_suggestion_genres", joinColumns = @JoinColumn(name = "suggestion_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "genre", nullable = false, length = 20)
    private Set<Genre> genres = new HashSet<>();

    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean shortlisted = true;

    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean agreed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MovieSuggestion() {
    }

    public MovieSuggestion(DatePlan datePlan, CurrentUser by, String title, int releaseYear,
                           String synopsis, String youtubeUrl, Set<Genre> genres) {
        this.datePlan = datePlan;
        this.suggestedById = by.id();
        this.suggestedByRole = by.role();
        this.title = title;
        this.releaseYear = releaseYear;
        this.synopsis = synopsis;
        this.youtubeUrl = youtubeUrl;
        if (genres != null) {
            this.genres.addAll(genres);
        }
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void markAgreed() {
        this.agreed = true;
        this.shortlisted = true;
    }

    public void moveToShelf() {
        this.shortlisted = false;
        this.agreed = false;
    }

    public void bringBack(CurrentUser by) {
        this.shortlisted = true;
        this.agreed = false;
        this.suggestedById = by.id();
        this.suggestedByRole = by.role();
    }

    public UUID getId() { return id; }
    public DatePlan getDatePlan() { return datePlan; }
    public UUID getSuggestedById() { return suggestedById; }
    public Role getSuggestedByRole() { return suggestedByRole; }
    public String getTitle() { return title; }
    public int getReleaseYear() { return releaseYear; }
    public String getSynopsis() { return synopsis; }
    public String getYoutubeUrl() { return youtubeUrl; }
    public Set<Genre> getGenres() { return genres; }
    public boolean isShortlisted() { return shortlisted; }
    public boolean isAgreed() { return agreed; }
    public Instant getCreatedAt() { return createdAt; }
}

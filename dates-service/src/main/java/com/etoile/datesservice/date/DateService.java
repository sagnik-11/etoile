package com.etoile.datesservice.date;

import com.etoile.datesservice.date.dto.CreateMeetingDateRequest;
import com.etoile.datesservice.date.dto.CreateMovieDateRequest;
import com.etoile.datesservice.date.dto.CreateTripDateRequest;
import com.etoile.datesservice.date.dto.DatePlanResponse;
import com.etoile.datesservice.date.dto.SuggestionRequest;
import com.etoile.datesservice.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DateService {

    private static final int MAX_SUGGESTIONS_EACH = 2;
    private static final Duration SUGGESTION_CUTOFF = Duration.ofHours(5);

    private final DatePlanRepository datePlans;
    private final MovieSuggestionRepository suggestions;

    public DateService(DatePlanRepository datePlans, MovieSuggestionRepository suggestions) {
        this.datePlans = datePlans;
        this.suggestions = suggestions;
    }

    @Transactional
    public DatePlanResponse createMovie(CurrentUser me, CreateMovieDateRequest req) {
        DatePlan date = datePlans.save(DatePlan.movie(me, req.startsAt()));
        List<MovieSuggestion> saved = req.suggestions() == null ? List.of() :
                suggestions.saveAll(req.suggestions().stream()
                        .map(s -> new MovieSuggestion(date, me, s.title().trim(), s.releaseYear(),
                                s.synopsis(), s.youtubeUrl(), s.genres()))
                        .toList());
        return DatePlanResponse.from(date, saved);
    }

    @Transactional
    public DatePlanResponse createMeeting(CurrentUser me, CreateMeetingDateRequest req) {
        DatePlan date = datePlans.save(DatePlan.meeting(me, req.startsAt(), req.place().trim(),
                req.mapsLink(), req.activity()));
        return DatePlanResponse.from(date, List.of());
    }

    @Transactional
    public DatePlanResponse createTrip(CurrentUser me, CreateTripDateRequest req) {
        if (!req.endsAt().isAfter(req.startsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trip must end after it starts");
        }
        DatePlan date = datePlans.save(DatePlan.trip(me, req.startsAt(), req.endsAt(), req.place().trim(),
                req.itinerary()));
        return DatePlanResponse.from(date, List.of());
    }

    @Transactional
    public DatePlanResponse accept(CurrentUser me, UUID id) {
        DatePlan date = findDate(id);
        requireReceiver(me, date);
        requireStatus(date, DateStatus.PROPOSED);
        requireNotStarted(date);
        date.accept();
        return toResponse(date);
    }

    @Transactional
    public DatePlanResponse reject(CurrentUser me, UUID id) {
        DatePlan date = findDate(id);
        requireReceiver(me, date);
        requireStatus(date, DateStatus.PROPOSED);
        requireNotStarted(date);
        date.reject();
        return toResponse(date);
    }

    @Transactional
    public DatePlanResponse cancel(CurrentUser me, UUID id) {
        DatePlan date = findDate(id);
        requireStatus(date, DateStatus.PROPOSED, DateStatus.ACCEPTED);
        if (date.getStatus() == DateStatus.PROPOSED && me.role() != date.getProposerRole()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not accepted yet, so reject it instead");
        }
        requireNotStarted(date);
        date.cancel(me.role());
        return toResponse(date);
    }

    @Transactional
    public DatePlanResponse addSuggestion(CurrentUser me, UUID id, SuggestionRequest req) {
        DatePlan date = findDate(id);
        requireMovie(date);
        requireStatus(date, DateStatus.ACCEPTED);
        requireSuggestionWindowOpen(date);
        if (suggestions.countByDatePlanIdAndSuggestedByRole(id, me.role()) >= MAX_SUGGESTIONS_EACH) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have 2 suggestions");
        }
        suggestions.save(new MovieSuggestion(date, me, req.title().trim(), req.releaseYear(),
                req.synopsis(), req.youtubeUrl(), req.genres()));
        return toResponse(date);
    }

    @Transactional
    public DatePlanResponse removeSuggestion(CurrentUser me, UUID id, UUID suggestionId) {
        DatePlan date = findDate(id);
        requireStatus(date, DateStatus.PROPOSED, DateStatus.ACCEPTED);
        requireSuggestionWindowOpen(date);
        MovieSuggestion suggestion = suggestions.findById(suggestionId)
                .filter(s -> s.getDatePlan().getId().equals(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Suggestion not found"));
        if (suggestion.getSuggestedByRole() != me.role()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only remove your own suggestion");
        }
        suggestions.delete(suggestion);
        return toResponse(date);
    }

    private void requireMovie(DatePlan date) {
        if (date.getType() != DateType.MOVIE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only movie dates have suggestions");
        }
    }

    private void requireSuggestionWindowOpen(DatePlan date) {
        if (Instant.now().plus(SUGGESTION_CUTOFF).isAfter(date.getStartsAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Suggestions close 5 hours before the date");
        }
    }

    private DatePlan findDate(UUID id) {
        return datePlans.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Date not found"));
    }

    private void requireReceiver(CurrentUser me, DatePlan date) {
        if (me.role() == date.getProposerRole()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the invited person can do this");
        }
    }

    private void requireStatus(DatePlan date, DateStatus... allowed) {
        if (!List.of(allowed).contains(date.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Date is " + date.getStatus());
        }
    }

    private void requireNotStarted(DatePlan date) {
        if (!date.getStartsAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Date has already started");
        }
    }

    private DatePlanResponse toResponse(DatePlan date) {
        return DatePlanResponse.from(date, suggestions.findByDatePlanIdOrderByCreatedAtAsc(date.getId()));
    }
}

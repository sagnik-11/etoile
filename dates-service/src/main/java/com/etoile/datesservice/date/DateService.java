package com.etoile.datesservice.date;

import com.etoile.datesservice.date.dto.AgreeRequest;
import com.etoile.datesservice.date.dto.CreateMeetingDateRequest;
import com.etoile.datesservice.date.dto.CreateMovieDateRequest;
import com.etoile.datesservice.date.dto.CreateTripDateRequest;
import com.etoile.datesservice.date.dto.DatePlanResponse;
import com.etoile.datesservice.date.dto.NewTimeRequest;
import com.etoile.datesservice.date.dto.SuggestionRequest;
import com.etoile.datesservice.date.dto.VoteRequest;
import com.etoile.datesservice.security.CurrentUser;
import com.etoile.datesservice.security.Role;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class DateService {

    private static final int MAX_SUGGESTIONS_EACH = 2;
    private static final Duration SUGGESTION_CUTOFF = Duration.ofHours(5);
    private static final int HER_WEIGHT_PERCENT = 60;

    private final DatePlanRepository datePlans;
    private final MovieSuggestionRepository suggestions;
    private final RescheduleRepository reschedules;
    private final VoteRepository votes;

    public DateService(DatePlanRepository datePlans, MovieSuggestionRepository suggestions,
                       RescheduleRepository reschedules, VoteRepository votes) {
        this.datePlans = datePlans;
        this.suggestions = suggestions;
        this.reschedules = reschedules;
        this.votes = votes;
    }

    // ---------- Create ----------

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

    // ---------- View ----------

    @Transactional(readOnly = true)
    public List<DatePlanResponse> list(CurrentUser me, String scope) {
        Instant now = Instant.now();
        if ("past".equals(scope)) {
            return datePlans.findAllByOrderByStartsAtDesc().stream()
                    .filter(d -> !isUpcoming(d, now))
                    .map(d -> toResponse(me, d))
                    .toList();
        }
        if (!"upcoming".equals(scope)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scope must be 'upcoming' or 'past'");
        }
        return datePlans.findByStatusInOrderByStartsAtAsc(List.of(DateStatus.PROPOSED, DateStatus.ACCEPTED)).stream()
                .filter(d -> isUpcoming(d, now))
                .map(d -> toResponse(me, d))
                .toList();
    }

    @Transactional(readOnly = true)
    public DatePlanResponse get(CurrentUser me, UUID id) {
        return toResponse(me, findDate(id));
    }

    private static boolean isUpcoming(DatePlan d, Instant now) {
        boolean active = d.getStatus() == DateStatus.PROPOSED || d.getStatus() == DateStatus.ACCEPTED;
        Instant end = d.getEndsAt() != null ? d.getEndsAt() : d.getStartsAt();
        return active && end.isAfter(now);
    }

    // ---------- Accept / reject / cancel ----------

    @Transactional
    public DatePlanResponse accept(CurrentUser me, UUID id) {
        DatePlan date = findDate(id);
        requireReceiver(me, date);
        requireStatus(date, DateStatus.PROPOSED);
        requireNotStarted(date);
        date.accept();
        return toResponse(me, date);
    }

    @Transactional
    public DatePlanResponse reject(CurrentUser me, UUID id) {
        DatePlan date = findDate(id);
        requireReceiver(me, date);
        requireStatus(date, DateStatus.PROPOSED);
        requireNotStarted(date);
        date.reject();
        return toResponse(me, date);
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
        reschedules.findByDatePlanIdAndStatus(id, RescheduleStatus.OPEN).ifPresent(r -> r.cancel(me.role()));
        return toResponse(me, date);
    }

    // ---------- Reschedule ----------

    @Transactional
    public DatePlanResponse requestReschedule(CurrentUser me, UUID id) {
        DatePlan date = findDate(id);
        requireStatus(date, DateStatus.ACCEPTED);
        requireNotStarted(date);
        if (reschedules.findByDatePlanIdAndStatus(id, RescheduleStatus.OPEN).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A reschedule is already in progress");
        }
        reschedules.save(new Reschedule(date, me.role()));
        return toResponse(me, date);
    }

    @Transactional
    public DatePlanResponse acknowledgeReschedule(CurrentUser me, UUID id) {
        DatePlan date = findDate(id);
        Reschedule r = findOpenReschedule(id);
        if (r.getRequestedBy() == me.role()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You asked for this reschedule; the other person acknowledges it");
        }
        r.acknowledge();
        return toResponse(me, date);
    }

    @Transactional
    public DatePlanResponse setNewTime(CurrentUser me, UUID id, NewTimeRequest req) {
        DatePlan date = findDate(id);
        Reschedule r = findOpenReschedule(id);
        if (r.getAcknowledgedAt() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Waiting for the other person to acknowledge first");
        }
        Instant endsAt = null;
        if (date.getType() == DateType.TRIP) {
            if (req.endsAt() == null || !req.endsAt().isAfter(req.startsAt())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trip needs an end time after its start");
            }
            endsAt = req.endsAt();
        }
        date.moveTo(req.startsAt(), endsAt);
        r.resolve(me.role(), req.startsAt());
        return toResponse(me, date);
    }

    @Transactional
    public DatePlanResponse cancelReschedule(CurrentUser me, UUID id) {
        DatePlan date = findDate(id);
        Reschedule r = findOpenReschedule(id);
        if (r.getRequestedBy() != me.role()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the person who asked can cancel the reschedule");
        }
        r.cancel(me.role());
        return toResponse(me, date);
    }

    private Reschedule findOpenReschedule(UUID dateId) {
        return reschedules.findByDatePlanIdAndStatus(dateId, RescheduleStatus.OPEN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No reschedule in progress"));
    }

    // ---------- Shortlist and shelf ----------

    @Transactional
    public DatePlanResponse addSuggestion(CurrentUser me, UUID id, SuggestionRequest req) {
        DatePlan date = findDate(id);
        requireMovie(date);
        requireStatus(date, DateStatus.ACCEPTED);
        requireNotLocked(date);
        requireSuggestionsOpen(date);
        requireFreeSlot(me, id);
        suggestions.save(new MovieSuggestion(date, me, req.title().trim(), req.releaseYear(),
                req.synopsis(), req.youtubeUrl(), req.genres()));
        resetVoting(date);
        return toResponse(me, date);
    }

    @Transactional
    public DatePlanResponse shelveSuggestion(CurrentUser me, UUID id, UUID suggestionId) {
        DatePlan date = findDate(id);
        requireStatus(date, DateStatus.PROPOSED, DateStatus.ACCEPTED);
        requireNotStarted(date);
        requireNotLocked(date);
        MovieSuggestion s = findSuggestion(id, suggestionId);
        if (!s.isShortlisted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already on the shelf");
        }
        // Your own suggestions, or the agreed movie (either of you).
        if (!s.isAgreed() && s.getSuggestedByRole() != me.role()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only remove your own suggestion");
        }
        boolean affectsVoting = votes.existsByDatePlanIdAndSuggestionId(id, suggestionId)
                || (date.getDecidedMovie() != null && date.getDecidedMovie().getId().equals(suggestionId));
        s.moveToShelf();
        if (affectsVoting) {
            resetVoting(date);
        }
        return toResponse(me, date);
    }

    @Transactional
    public DatePlanResponse bringBack(CurrentUser me, UUID id, UUID suggestionId) {
        DatePlan date = findDate(id);
        requireMovie(date);
        requireStatus(date, DateStatus.ACCEPTED);
        requireNotLocked(date);
        requireSuggestionsOpen(date);
        MovieSuggestion s = findSuggestion(id, suggestionId);
        if (s.isShortlisted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already on the shortlist");
        }
        requireFreeSlot(me, id);
        s.bringBack(me);
        resetVoting(date);
        return toResponse(me, date);
    }

    // ---------- Voting, call decision, random pick ----------

    @Transactional
    public DatePlanResponse vote(CurrentUser me, UUID id, VoteRequest req) {
        DatePlan date = findDate(id);
        requireMovieDecisionOpen(date);
        if (date.getDecisionMethod() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already decided. Add or bring back a movie to vote again");
        }
        if (date.getHerPick() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The vote failed. Agree after your call, or pick randomly");
        }
        List<MovieSuggestion> running = suggestions.findByDatePlanIdAndShortlistedTrue(id);
        if (running.size() < 2) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Need at least 2 movies to vote");
        }
        MovieSuggestion choice = findSuggestion(id, req.suggestionId());
        if (!choice.isShortlisted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That movie is on the shelf");
        }

        Optional<Vote> mine = votes.findByDatePlanIdAndVoterRole(id, me.role());
        Optional<Vote> theirs = votes.findByDatePlanIdAndVoterRole(id, me.role().other());
        if (mine.isPresent()) {
            mine.get().changeTo(choice);
        } else {
            votes.save(new Vote(date, me.role(), choice));
        }
        date.startVoting();

        if (theirs.isPresent()) {
            MovieSuggestion other = theirs.get().getSuggestion();
            if (other.getId().equals(choice.getId())) {
                date.decide(choice, DecisionMethod.VOTE);
            } else {
                MovieSuggestion herPick = me.role() == Role.HER ? choice : other;
                MovieSuggestion himPick = me.role() == Role.HIM ? choice : other;
                date.narrow(herPick, himPick);
                running.stream()
                        .filter(s -> !s.getId().equals(herPick.getId()) && !s.getId().equals(himPick.getId()))
                        .forEach(MovieSuggestion::moveToShelf);
            }
        }
        return toResponse(me, date);
    }

    @Transactional
    public DatePlanResponse agree(CurrentUser me, UUID id, AgreeRequest req) {
        DatePlan date = findDate(id);
        requireMovieDecisionOpen(date);
        if (date.getHerPick() == null || date.getDecisionMethod() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You can record this after a vote fails");
        }
        if ((req.suggestionId() == null) == (req.newMovie() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pick an existing movie or add a new one, not both");
        }
        MovieSuggestion chosen;
        if (req.suggestionId() != null) {
            chosen = findSuggestion(id, req.suggestionId());
        } else {
            SuggestionRequest m = req.newMovie();
            chosen = suggestions.save(new MovieSuggestion(date, me, m.title().trim(), m.releaseYear(),
                    m.synopsis(), m.youtubeUrl(), m.genres()));
        }
        boolean wasNarrowed = chosen.getId().equals(date.getHerPick().getId())
                || chosen.getId().equals(date.getHimPick().getId());
        suggestions.findByDatePlanIdAndShortlistedTrue(id).stream()
                .filter(s -> !s.getId().equals(chosen.getId()))
                .forEach(MovieSuggestion::moveToShelf);
        chosen.markAgreed();
        date.decide(chosen, wasNarrowed ? DecisionMethod.AGREED_NARROWED : DecisionMethod.AGREED_OTHER);
        return toResponse(me, date);
    }

    @Transactional
    public DatePlanResponse randomPick(CurrentUser me, UUID id) {
        DatePlan date = findDate(id);
        requireMovieDecisionOpen(date);
        if (date.getHerPick() == null || date.getDecisionMethod() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Random pick is only for after a failed vote");
        }
        // nextInt(100) gives 0..99, so "below 60" happens 60 times out of 100.
        boolean herWins = ThreadLocalRandom.current().nextInt(100) < HER_WEIGHT_PERCENT;
        MovieSuggestion winner = herWins ? date.getHerPick() : date.getHimPick();
        MovieSuggestion loser = herWins ? date.getHimPick() : date.getHerPick();
        loser.moveToShelf();
        date.decide(winner, DecisionMethod.RANDOM);
        return toResponse(me, date);
    }

    // ---------- Rule checks ----------

    private void requireMovieDecisionOpen(DatePlan date) {
        requireMovie(date);
        requireStatus(date, DateStatus.ACCEPTED);
        requireNotStarted(date);
        requireNotLocked(date);
    }

    private void requireNotLocked(DatePlan date) {
        if (date.isLocked()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The app has spoken. The random pick is final");
        }
    }

    private void resetVoting(DatePlan date) {
        votes.deleteByDatePlanId(date.getId());
        date.resetDecision();
    }

    private void requireSuggestionsOpen(DatePlan date) {
        if (!date.isVotingStarted()) {
            requireSuggestionWindowOpen(date);   // first round: the 5-hour rule
        } else {
            requireNotStarted(date);             // after the first vote: until the date starts
        }
    }

    private void requireFreeSlot(CurrentUser me, UUID dateId) {
        if (suggestions.countByDatePlanIdAndSuggestedByRoleAndShortlistedTrueAndAgreedFalse(dateId, me.role()) >= MAX_SUGGESTIONS_EACH) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have 2 movies on the shortlist");
        }
    }

    private MovieSuggestion findSuggestion(UUID dateId, UUID suggestionId) {
        return suggestions.findById(suggestionId)
                .filter(s -> s.getDatePlan().getId().equals(dateId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found"));
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

    // The reply is built for whoever is asking, so the other person's vote can stay secret.
    private DatePlanResponse toResponse(CurrentUser me, DatePlan date) {
        return DatePlanResponse.from(date,
                suggestions.findByDatePlanIdOrderByCreatedAtAsc(date.getId()),
                reschedules.findByDatePlanIdAndStatus(date.getId(), RescheduleStatus.OPEN).orElse(null),
                votes.findByDatePlanId(date.getId()),
                me.role());
    }
}

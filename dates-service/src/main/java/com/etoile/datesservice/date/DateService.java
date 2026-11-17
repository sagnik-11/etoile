package com.etoile.datesservice.date;

import com.etoile.datesservice.date.dto.CreateMeetingDateRequest;
import com.etoile.datesservice.date.dto.CreateMovieDateRequest;
import com.etoile.datesservice.date.dto.CreateTripDateRequest;
import com.etoile.datesservice.date.dto.DatePlanResponse;
import com.etoile.datesservice.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DateService {

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
}

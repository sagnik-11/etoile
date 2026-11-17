package com.etoile.datesservice.date;

import com.etoile.datesservice.date.dto.AgreeRequest;
import com.etoile.datesservice.date.dto.CreateMeetingDateRequest;
import com.etoile.datesservice.date.dto.CreateMovieDateRequest;
import com.etoile.datesservice.date.dto.CreateTripDateRequest;
import com.etoile.datesservice.date.dto.DatePlanResponse;
import com.etoile.datesservice.date.dto.NewTimeRequest;
import com.etoile.datesservice.date.dto.SuggestionRequest;
import com.etoile.datesservice.date.dto.CancelRequest;
import com.etoile.datesservice.date.dto.VoteRequest;
import com.etoile.datesservice.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dates")
public class DateController {

    private final DateService dateService;

    public DateController(DateService dateService) {
        this.dateService = dateService;
    }

    @GetMapping
    public List<DatePlanResponse> list(@AuthenticationPrincipal Jwt jwt,
                                       @RequestParam(defaultValue = "upcoming") String scope) {
        return dateService.list(CurrentUser.from(jwt), scope);
    }

    @GetMapping("/{id}")
    public DatePlanResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return dateService.get(CurrentUser.from(jwt), id);
    }

    @PostMapping("/movie")
    @ResponseStatus(HttpStatus.CREATED)
    public DatePlanResponse createMovie(@AuthenticationPrincipal Jwt jwt,
                                        @Valid @RequestBody CreateMovieDateRequest request) {
        return dateService.createMovie(CurrentUser.from(jwt), request);
    }

    @PostMapping("/meeting")
    @ResponseStatus(HttpStatus.CREATED)
    public DatePlanResponse createMeeting(@AuthenticationPrincipal Jwt jwt,
                                          @Valid @RequestBody CreateMeetingDateRequest request) {
        return dateService.createMeeting(CurrentUser.from(jwt), request);
    }

    @PostMapping("/trip")
    @ResponseStatus(HttpStatus.CREATED)
    public DatePlanResponse createTrip(@AuthenticationPrincipal Jwt jwt,
                                       @Valid @RequestBody CreateTripDateRequest request) {
        return dateService.createTrip(CurrentUser.from(jwt), request);
    }

    @PostMapping("/{id}/accept")
    public DatePlanResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return dateService.accept(CurrentUser.from(jwt), id);
    }

    @PostMapping("/{id}/reject")
    public DatePlanResponse reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return dateService.reject(CurrentUser.from(jwt), id);
    }

    @PostMapping("/{id}/cancel")
    public DatePlanResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                   @Valid @RequestBody CancelRequest request) {
        return dateService.cancel(CurrentUser.from(jwt), id);
    }

    @PostMapping("/{id}/suggestions")
    @ResponseStatus(HttpStatus.CREATED)
    public DatePlanResponse addSuggestion(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                          @Valid @RequestBody SuggestionRequest request) {
        return dateService.addSuggestion(CurrentUser.from(jwt), id, request);
    }

    @PostMapping("/{id}/suggestions/{suggestionId}/shelve")
    public DatePlanResponse shelve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                   @PathVariable UUID suggestionId) {
        return dateService.shelveSuggestion(CurrentUser.from(jwt), id, suggestionId);
    }

    @PostMapping("/{id}/suggestions/{suggestionId}/bring-back")
    public DatePlanResponse bringBack(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                      @PathVariable UUID suggestionId) {
        return dateService.bringBack(CurrentUser.from(jwt), id, suggestionId);
    }

    @PostMapping("/{id}/vote")
    public DatePlanResponse vote(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                 @Valid @RequestBody VoteRequest request) {
        return dateService.vote(CurrentUser.from(jwt), id, request);
    }

    @PostMapping("/{id}/agree")
    public DatePlanResponse agree(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                  @Valid @RequestBody AgreeRequest request) {
        return dateService.agree(CurrentUser.from(jwt), id, request);
    }

    @PostMapping("/{id}/random-pick")
    public DatePlanResponse randomPick(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return dateService.randomPick(CurrentUser.from(jwt), id);
    }

    @PostMapping("/{id}/reschedule")
    public DatePlanResponse requestReschedule(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return dateService.requestReschedule(CurrentUser.from(jwt), id);
    }

    @PostMapping("/{id}/reschedule/acknowledge")
    public DatePlanResponse acknowledgeReschedule(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return dateService.acknowledgeReschedule(CurrentUser.from(jwt), id);
    }

    @PostMapping("/{id}/reschedule/new-time")
    public DatePlanResponse setNewTime(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                       @Valid @RequestBody NewTimeRequest request) {
        return dateService.setNewTime(CurrentUser.from(jwt), id, request);
    }

    @PostMapping("/{id}/reschedule/cancel")
    public DatePlanResponse cancelReschedule(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return dateService.cancelReschedule(CurrentUser.from(jwt), id);
    }
}

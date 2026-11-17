package com.etoile.datesservice.date;

import com.etoile.datesservice.date.dto.CreateMeetingDateRequest;
import com.etoile.datesservice.date.dto.CreateMovieDateRequest;
import com.etoile.datesservice.date.dto.CreateTripDateRequest;
import com.etoile.datesservice.date.dto.DatePlanResponse;
import com.etoile.datesservice.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dates")
public class DateController {

    private final DateService dateService;

    public DateController(DateService dateService) {
        this.dateService = dateService;
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
}

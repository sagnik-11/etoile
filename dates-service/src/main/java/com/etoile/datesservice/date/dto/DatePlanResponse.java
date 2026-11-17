package com.etoile.datesservice.date.dto;

import com.etoile.datesservice.date.DatePlan;
import com.etoile.datesservice.date.DateStatus;
import com.etoile.datesservice.date.DateType;
import com.etoile.datesservice.date.MovieSuggestion;
import com.etoile.datesservice.security.Role;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DatePlanResponse(
        UUID id,
        DateType type,
        DateStatus status,
        Role proposedBy,
        Instant startsAt,
        Instant endsAt,
        String place,
        String mapsLink,
        String activity,
        String itinerary,
        Instant createdAt,
        List<SuggestionResponse> suggestions
) {
    public static DatePlanResponse from(DatePlan d, List<MovieSuggestion> suggestions) {
        return new DatePlanResponse(d.getId(), d.getType(), d.getStatus(), d.getProposerRole(),
                d.getStartsAt(), d.getEndsAt(), d.getPlace(), d.getMapsLink(), d.getActivity(),
                d.getItinerary(), d.getCreatedAt(),
                suggestions.stream().map(SuggestionResponse::from).toList());
    }
}

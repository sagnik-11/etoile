package com.etoile.datesservice.date.dto;

import com.etoile.datesservice.date.DatePlan;
import com.etoile.datesservice.date.DateStatus;
import com.etoile.datesservice.date.DateType;
import com.etoile.datesservice.date.DecisionMethod;
import com.etoile.datesservice.date.MovieSuggestion;
import com.etoile.datesservice.date.Reschedule;
import com.etoile.datesservice.date.Vote;
import com.etoile.datesservice.security.Role;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DatePlanResponse(
        UUID id,
        DateType type,
        DateStatus status,
        Role proposedBy,
        Role cancelledBy,
        Instant startsAt,
        Instant endsAt,
        String place,
        String mapsLink,
        String activity,
        String itinerary,
        Instant createdAt,
        List<SuggestionResponse> shortlist,
        List<SuggestionResponse> shelf,
        DecisionResponse decision,
        VotingResponse voting,
        RescheduleResponse reschedule
) {
    public static DatePlanResponse from(DatePlan d, List<MovieSuggestion> suggestions) {
        return from(d, suggestions, null, List.of(), null);
    }

    public static DatePlanResponse from(DatePlan d, List<MovieSuggestion> suggestions, Reschedule openReschedule,
                                        List<Vote> votes, Role viewer) {
        List<MovieSuggestion> running = suggestions.stream().filter(MovieSuggestion::isShortlisted).toList();
        return new DatePlanResponse(d.getId(), d.getType(), d.getStatus(), d.getProposerRole(), d.getCancelledBy(),
                d.getStartsAt(), d.getEndsAt(), d.getPlace(), d.getMapsLink(), d.getActivity(),
                d.getItinerary(), d.getCreatedAt(),
                running.stream().map(SuggestionResponse::from).toList(),
                suggestions.stream().filter(s -> !s.isShortlisted()).map(SuggestionResponse::from).toList(),
                decisionOf(d, running),
                votingOf(d, votes, viewer),
                RescheduleResponse.from(openReschedule));
    }

    private static DecisionResponse decisionOf(DatePlan d, List<MovieSuggestion> running) {
        if (d.getType() != DateType.MOVIE) {
            return null;
        }
        UUID herPick = d.getHerPick() == null ? null : d.getHerPick().getId();
        UUID himPick = d.getHimPick() == null ? null : d.getHimPick().getId();
        if (d.getDecidedMovie() != null) {
            return new DecisionResponse(d.getDecidedMovie().getId(), d.getDecidedMovie().getTitle(),
                    d.getDecisionMethod(), d.isLocked(), herPick, himPick);
        }
        if (running.size() == 1 && herPick == null) {
            MovieSuggestion only = running.get(0);
            return new DecisionResponse(only.getId(), only.getTitle(), DecisionMethod.ONLY_ONE, false, null, null);
        }
        return new DecisionResponse(null, null, null, false, herPick, himPick);
    }

    // Votes are secret: you only see the other person's vote once both of you have voted.
    private static VotingResponse votingOf(DatePlan d, List<Vote> votes, Role viewer) {
        if (d.getType() != DateType.MOVIE) {
            return null;
        }
        Vote him = votes.stream().filter(v -> v.getVoterRole() == Role.HIM).findFirst().orElse(null);
        Vote her = votes.stream().filter(v -> v.getVoterRole() == Role.HER).findFirst().orElse(null);
        boolean bothVoted = him != null && her != null;
        UUID himVote = him != null && (bothVoted || viewer == Role.HIM) ? him.getSuggestion().getId() : null;
        UUID herVote = her != null && (bothVoted || viewer == Role.HER) ? her.getSuggestion().getId() : null;
        return new VotingResponse(him != null, her != null, himVote, herVote);
    }
}

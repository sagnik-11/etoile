package com.etoile.datesservice.date.dto;

import java.util.UUID;

public record VotingResponse(
        boolean himVoted,
        boolean herVoted,
        UUID himVote,   // hidden (null) from HER until both have voted
        UUID herVote    // hidden (null) from HIM until both have voted
) {
}

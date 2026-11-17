package com.etoile.datesservice.date.dto;

import com.etoile.datesservice.date.Genre;
import com.etoile.datesservice.date.MovieSuggestion;
import com.etoile.datesservice.security.Role;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public record SuggestionResponse(
        UUID id,
        Role suggestedBy,
        String title,
        int releaseYear,
        String synopsis,
        String youtubeUrl,
        Set<Genre> genres
) {
    public static SuggestionResponse from(MovieSuggestion s) {
        // EnumSet keeps genres in the order they are listed in Genre, so capsules don't shuffle.
        Set<Genre> genres = s.getGenres().isEmpty() ? EnumSet.noneOf(Genre.class) : EnumSet.copyOf(s.getGenres());
        return new SuggestionResponse(s.getId(), s.getSuggestedByRole(), s.getTitle(), s.getReleaseYear(),
                s.getSynopsis(), s.getYoutubeUrl(), genres);
    }
}

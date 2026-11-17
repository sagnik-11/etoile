package com.etoile.datesservice.date.dto;

import com.etoile.datesservice.date.Genre;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record SuggestionRequest(
        @NotBlank @Size(max = 255) String title,
        @NotNull @Min(1888) @Max(2100) Integer releaseYear,
        @Size(max = 5000) String synopsis,
        @Size(max = 1000)
        @Pattern(regexp = "^https://(www\\.|m\\.)?(youtube\\.com|youtu\\.be)/.+$", message = "must be a YouTube link")
        String youtubeUrl,
        @Size(max = 5) Set<Genre> genres
) {
}

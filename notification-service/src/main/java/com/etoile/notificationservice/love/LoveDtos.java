package com.etoile.notificationservice.love;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class LoveDtos {

    private LoveDtos() {
    }

    // taps: how many times the heart was tapped in one burst (the app groups quick taps).
    public record SendLoveRequest(@NotNull @Min(1) @Max(100) Integer taps) {
    }

    // loveReceived: ×N shown on your love icon. theyNeedYou: the other person pressed "Need love".
    // youAreWaiting: you pressed "Need love" and they haven't opened it yet.
    public record LoveResponse(int loveReceived, boolean theyNeedYou, boolean youAreWaiting) {
    }
}

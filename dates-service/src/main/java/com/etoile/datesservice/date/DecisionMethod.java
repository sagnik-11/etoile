package com.etoile.datesservice.date;

public enum DecisionMethod {
    ONLY_ONE,         // only one movie in the running (worked out, never stored)
    VOTE,             // you both voted for the same movie
    AGREED_NARROWED,  // after the call, one of the 2 narrowed  -> "We talked, we agreed"
    AGREED_OTHER,     // after the call, a shelf or new movie   -> "Okay fine, this one"
    RANDOM            // 60/40 random pick, final
}

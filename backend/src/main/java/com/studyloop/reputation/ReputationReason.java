package com.studyloop.reputation;

public enum ReputationReason {
    ANSWER_ACCEPTED(15),
    FAST_ANSWER(5),
    HELPFUL_RATING(5),
    LOOP_CLOSED(2);

    private final int delta;

    ReputationReason(int delta) {
        this.delta = delta;
    }

    public int delta() {
        return delta;
    }
}

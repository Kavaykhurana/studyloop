package com.studyloop.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/** A clock tests can move forward. */
public class MutableClock extends Clock {

    private volatile Instant now = start();
    private final ZoneId zone;

    public MutableClock() {
        this(ZoneOffset.UTC);
    }

    private MutableClock(ZoneId zone) {
        this.zone = zone;
    }

    private static Instant start() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public void reset() {
        now = start();
    }

    public void advance(Duration d) {
        now = now.plus(d);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        MutableClock view = new MutableClock(zone) {
            @Override
            public Instant instant() {
                return MutableClock.this.instant();
            }
        };
        return view;
    }

    @Override
    public Instant instant() {
        return now;
    }
}

/*
 *
 *  Copyright 2016 Robert Winkler and Bohdan Storozhuk
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.ratelimiter.event;

import java.time.Duration;

public class RateLimiterOnFailureEvent extends AbstractRateLimiterEvent {

    private final int availablePermissions;
    private final Duration timeoutDuration;

    public RateLimiterOnFailureEvent(String rateLimiterName) {
        this(rateLimiterName, 1);
    }

    public RateLimiterOnFailureEvent(String rateLimiterName, int numberOfPermits) {
        this(rateLimiterName, numberOfPermits, 0, Duration.ZERO);
    }

    public RateLimiterOnFailureEvent(String rateLimiterName, int numberOfPermits,
                                     int availablePermissions, Duration timeoutDuration) {
        super(rateLimiterName, numberOfPermits);
        this.availablePermissions = availablePermissions;
        this.timeoutDuration = timeoutDuration;
    }

    /** Returns the available permissions at the time acquisition failed. */
    public int getAvailablePermissions() { return availablePermissions; }

    /** Returns the configured timeout used for this acquisition. */
    public Duration getTimeoutDuration() { return timeoutDuration; }

    @Override
    public Type getEventType() {
        return Type.FAILED_ACQUIRE;
    }
}

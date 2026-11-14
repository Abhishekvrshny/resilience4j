/*
 *
 *  Copyright 2026 Robert Winkler and Bohdan Storozhuk
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
package io.github.resilience4j.ratelimiter;

import io.vavr.control.Try;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnFailureEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

class RateLimiterEventPublisherTest {

    private static final int LIMIT = 1;
    private static final Duration TIMEOUT = Duration.ZERO;
    private static final Duration REFRESH_PERIOD = Duration.ofSeconds(5);

    private RateLimiter rateLimiter;
    private Logger logger;

    @BeforeEach
    void setUp() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .timeoutDuration(TIMEOUT)
            .limitRefreshPeriod(REFRESH_PERIOD)
            .limitForPeriod(LIMIT)
            .build();
        rateLimiter = RateLimiter.of("test", config);
        logger = mock(Logger.class);
    }

    @Test
    void shouldReturnTheSameConsumer() {
        RateLimiter.EventPublisher eventPublisher = rateLimiter.getEventPublisher();
        RateLimiter.EventPublisher eventPublisher2 = rateLimiter.getEventPublisher();

        assertThat(eventPublisher).isEqualTo(eventPublisher2);
    }


    @Test
    void shouldConsumeOnSuccessEvent() {
        rateLimiter.getEventPublisher().onSuccess(
            event -> logger.info(event.getEventType().toString()));

        String result = rateLimiter.executeSupplier(() -> "Hello world");

        assertThat(result).isEqualTo("Hello world");
        then(logger).should(times(1)).info("SUCCESSFUL_ACQUIRE");
    }

    @Test
    void shouldConsumeOnFailureEvent() {
        CopyOnWriteArrayList<RateLimiterOnFailureEvent> events = new CopyOnWriteArrayList<>();
        rateLimiter.getEventPublisher().onFailure(event -> {
            events.add(event);
            logger.info(event.getEventType().toString());
        });
        rateLimiter.executeSupplier(() -> "Hello world");

        Try.ofSupplier(RateLimiter.decorateSupplier(rateLimiter, () -> "Hello world"));

        then(logger).should(times(1)).info("FAILED_ACQUIRE");
    }

    @Test
    public void shouldIncludeFailureDetailsAndCreationTime() {
        CopyOnWriteArrayList<RateLimiterOnFailureEvent> events = new CopyOnWriteArrayList<>();
        rateLimiter.getEventPublisher().onFailure(events::add);
        rateLimiter.acquirePermission();
        ZonedDateTime beforeRejection = ZonedDateTime.now();
        assertThat(rateLimiter.acquirePermission()).isFalse();

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getAvailablePermissions()).isEqualTo(0);
        assertThat(events.get(0).getTimeoutDuration()).isEqualTo(TIMEOUT);
        assertThat(events.get(0).getCreationTime()).isAfterOrEqualTo(beforeRejection);
    }

    @Test
    public void shouldPublishOneEventForEachConcurrentRejectedRequest() throws Exception {
        CopyOnWriteArrayList<RateLimiterOnFailureEvent> events = new CopyOnWriteArrayList<>();
        rateLimiter.getEventPublisher().onFailure(events::add);
        rateLimiter.acquirePermission();
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(8);
        try {
            java.util.List<java.util.concurrent.Future<Boolean>> results = new java.util.ArrayList<>();
            for (int i = 0; i < 20; i++) {
                results.add(executor.submit(() -> rateLimiter.acquirePermission()));
            }
            for (java.util.concurrent.Future<Boolean> result : results) {
                assertThat(result.get()).isFalse();
            }
        } finally {
            executor.shutdownNow();
        }
        assertThat(events).hasSize(20);
        assertThat(events).allSatisfy(event -> {
            assertThat(event.getAvailablePermissions()).isEqualTo(0);
            assertThat(event.getTimeoutDuration()).isEqualTo(TIMEOUT);
            assertThat(event.getCreationTime()).isNotNull();
        });
    }

}

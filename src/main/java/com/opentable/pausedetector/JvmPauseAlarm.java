/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opentable.pausedetector;

import java.io.Closeable;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

/**
 * This class performs a simple task, to try to detect a pause in a process (usually a JVM GC process).
 * To do so, it simply runs a background thread, and marks the time that elapsed before and after a
 * sleep interval. If this exceeds some threshold, the alarm is triggered by calling the onPause consumer.
 *
 * Note: This is only &quot;mostly&quot; correct. For 100% correctness you must use a strictly monotonic clock - e.g.
 * one that always increases and never goes backwards. Otherwise you can get both false positives and false negatives.
 *
 * System.nanoTime partially fills this requirement, but
 * moving backwards or clock skew has been reported. CLOCK_MONOTONIC_RAW a newish kernel call supposedly addresses
 * this but has not been applied to JVM.
 */
public class JvmPauseAlarm implements Runnable, Closeable
{
    private static final Logger LOG = LoggerFactory.getLogger(JvmPauseAlarm.class);
    private static final long S_THRESHOLD = 1000;

    private final long sleepTimeMs, alarmTimeMs;
    private final Consumer<Long> onPause;
    private final Supplier<Instant> clock;

    private Function<Long, Marker> markerCreator = l -> null; //NOPMD - I'm lazy and setting this final with a catch is PITA

    private volatile boolean running = true;

    /**
     * Create a JVM Pause Alarm
     * @param sleepTimeMs how often we should check to see if the JVM has paused too long (in milliseconds)
     * @param alarmTimeMs trigger alarm if pause exceeds this number of milliseconds
     */
    public JvmPauseAlarm(long sleepTimeMs, long alarmTimeMs) {
        this(sleepTimeMs, alarmTimeMs, l -> {});
    }

    /**
     * Create a JVM Pause Alarm
     * @param sleepTimeMs how often we should check to see if the JVM has paused too long (in milliseconds)
     * @param alarmTimeMs trigger alarm if pause exceeds this number of milliseconds
     * @param onPause when there is a pause exceeding {@link alarmTimeMs} call this consumer with the length of the pause in milliseconds
     */
    public JvmPauseAlarm(long sleepTimeMs, long alarmTimeMs, Consumer<Long> onPause) {
        this(Clock.systemUTC()::instant, sleepTimeMs, alarmTimeMs, onPause);
    }

    /**
     * Create a JVM Pause Alarm
     * @param clock the clock to use for measuring time
     * @param sleepTimeMs how often we should check to see if the JVM has paused too long (in milliseconds)
     * @param alarmTimeMs trigger alarm if pause exceeds this number of milliseconds
     * @param onPause when there is a pause exceeding {@link alarmTimeMs} call this consumer with the length of the pause in milliseconds
     */
    public JvmPauseAlarm(Supplier<Instant> clock, long sleepTimeMs, long alarmTimeMs, Consumer<Long> onPause) {
        this.clock = clock;
        this.sleepTimeMs = sleepTimeMs;
        this.alarmTimeMs = alarmTimeMs;
        this.onPause = onPause;

        try {
            markerCreator = new PauseMetadataFactory();
        } catch (Throwable t) { //NOPMD - NoClassDefFound Possible
            LOG.trace("Failed to initialize metadata", t);
        }
    }

    /**
     * Start a daemon thread to check for JVM pauses.
     * Will be executed automatically by the container after this bean is created.
     *
     * @return this pause alarm in a started state
     */
    @PostConstruct
    public JvmPauseAlarm start()
    {
        ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "jvm-pause-alarm");
                t.setDaemon(true);
                return t;
            }
        });
        executor.submit(this);
        executor.shutdown();
        return this;
    }

    @Override
    public void run()
    {
        try {
            safeRun();
        } catch (Exception e) {
            LOG.error("Exiting due to exception", e);
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    @Override
    public void close() throws IOException {
        LOG.info("Shutting down");
        running = false;
    }

    private void safeRun()
    {
        LOG.info("Watching JVM for GC pausing.  Checking every {} for pauses of at least {}.",
                formatTime(sleepTimeMs), formatTime(alarmTimeMs));

        long lastUpdate = clock.get().toEpochMilli();
        while (running) {
            try {
                Thread.sleep(sleepTimeMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.info("Exiting due to interrupt");
                return;
            }

            final long now = clock.get().toEpochMilli();
            final long pauseMs = now - lastUpdate;

            if (pauseMs > alarmTimeMs) {
                LOG.warn(markerCreator.apply(pauseMs), "Detected pause of {}!", formatTime(pauseMs));
                try {
                    onPause.accept(pauseMs);
                } catch (Exception e) {
                    LOG.error("While calling onPause handler {}", onPause, e);
                }
            }

            lastUpdate = now;
        }
        LOG.info("Terminated");
    }

    private String formatTime(final long time) {
        return time > S_THRESHOLD ? String.format("%.1fs", time / 1000.0) : time + "ms";
    }
}

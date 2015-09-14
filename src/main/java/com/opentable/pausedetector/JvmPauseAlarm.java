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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

public class JvmPauseAlarm implements Runnable, Closeable
{
    private static final Logger LOG = LoggerFactory.getLogger(JvmPauseAlarm.class);
    private static final long S_THRESHOLD = 1000;

    private final long sleepTimeMs, alarmTimeMs;
    private final Consumer<Long> onPause;
    private final Supplier<Instant> clock;

    private Function<Long, Marker> markerCreator = l -> null;

    private volatile boolean running = true;

    public JvmPauseAlarm(long sleepTimeMs, long alarmTimeMs) {
        this(sleepTimeMs, alarmTimeMs, l -> {});
    }

    public JvmPauseAlarm(long sleepTimeMs, long alarmTimeMs, Consumer<Long> onPause) {
        this(Clock.systemUTC()::instant, sleepTimeMs, alarmTimeMs, onPause);
    }

    public JvmPauseAlarm(Supplier<Instant> clock, long sleepTimeMs, long alarmTimeMs, Consumer<Long> onPause) {
        this.clock = clock;
        this.sleepTimeMs = sleepTimeMs;
        this.alarmTimeMs = alarmTimeMs;
        this.onPause = onPause;

        try {
            markerCreator = new PauseMetadataFactory();
        } catch (Throwable t) {
            LOG.trace("Failed to initialize metadata", t);
        }
    }

    public JvmPauseAlarm start()
    {
        ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("jvm-pause-alarm").setDaemon(true).build());
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
            throw Throwables.propagate(e);
        }
    }

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

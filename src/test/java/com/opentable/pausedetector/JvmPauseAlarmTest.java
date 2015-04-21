package com.opentable.pausedetector;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public class JvmPauseAlarmTest {
    @Test
    public void testJvmPauseAlarm() throws Exception {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.ofEpochMilli(1000));
        AtomicLong pause = new AtomicLong();
        try (JvmPauseAlarm alarm = new JvmPauseAlarm(now::get, 10, 400, pause::set)) {
            alarm.start();
            Thread.sleep(100);
            now.set(Instant.ofEpochMilli(2000));
            Thread.sleep(100);
        }
        assertEquals(1000, pause.get());
    }
}

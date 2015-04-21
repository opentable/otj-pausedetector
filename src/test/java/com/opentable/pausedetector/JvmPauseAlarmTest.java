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

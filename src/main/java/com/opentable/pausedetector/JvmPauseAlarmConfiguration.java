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

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration bean to configure, create, and expose the JVM Pause Alarm
 */
@Configuration
public class JvmPauseAlarmConfiguration
{
    /**
     * Turn the pause alarm on or off at config time. Can be configured via <tt>ot.jvm-pause.enabled</tt>. Defaults to true.
     */
    @Value("${ot.jvm-pause.enabled:true}")
    boolean isPauseAlarmEnabled;

    /**
     * The pause alarm will check this often to see if the JVM was taking a nap.
     * This time should be significantly smaller than the pause time, or you
     * may regret it...
     * Configured via <tt>ot.jvm-pause.check-time</tt>. Defaults to 0.05 seconds.
     */
    @Value("${ot.jvm-pause.check-time:PT0.05s}")
    Duration checkTime;

    /**
     * Report pauses that last longer than this amount.
     * Configured via <tt>ot.jvm-pause.pause-time</tt>. Defaults to 0.2 seconds.
     */
    @Value("${ot.jvm-pause.pause-time:PT0.2s}")
    Duration pauseAlarmTime;

    /**
     * The clock to use for measuring pauses. Will inject a clock bean here if there is one in the context.
     * Will default to {@link Clock#systemUTC()} if no clock bean is provided.
     */
    @Inject
    Optional<Clock> clock;

    /**
     * Creates a JVM Pause Alarm that uses the configured thresholds and clock (or defaults). On alarms no action will be taken, aside from logging.
     * @return a JVM Pause Alarm
     */
    @Bean
    public JvmPauseAlarm jvmPauseAlarm() {
        return new JvmPauseAlarm(clock.orElse(Clock.systemUTC())::instant, checkTime.toMillis(), pauseAlarmTime.toMillis(), (t) -> {});
    }
}

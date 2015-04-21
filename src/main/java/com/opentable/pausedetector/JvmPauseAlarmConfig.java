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

import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.TimeSpan;

interface JvmPauseAlarmConfig
{
    /**
     * Turn the pause alarm on or off at config time.
     */
    @Config("ot.jvm-pause.enabled")
    @Default("true")
    boolean isPauseAlarmEnabled();

    /**
     * The pause alarm will check this often to see if the JVM was taking a nap.
     * This time should be significantly smaller than the pause time, or you
     * may regret it...
     */
    @Config("ot.jvm-pause.check-time")
    @Default("50ms")
    TimeSpan getCheckTime();

    /**
     * Report pauses that last longer than this amount.
     */
    @Config("ot.jvm-pause.pause-time")
    @Default("200ms")
    TimeSpan getPauseAlarmTime();
}

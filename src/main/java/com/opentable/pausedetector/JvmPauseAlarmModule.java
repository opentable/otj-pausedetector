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

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Scopes;

import com.opentable.config.ConfigProvider;
import com.opentable.lifecycle.Lifecycle;
import com.opentable.lifecycle.LifecycleStage;

public class JvmPauseAlarmModule extends AbstractModule
{
    @Override
    public void configure()
    {
        bind (JvmPauseAlarm.class).toProvider(JvmPauseAlarmProvider.class).asEagerSingleton();
        bind (JvmPauseAlarmConfig.class).toProvider(ConfigProvider.of(JvmPauseAlarmConfig.class)).in(Scopes.SINGLETON);
    }

    @Override
    public int hashCode()
    {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj != null && obj.getClass() == getClass();
    }

    static class JvmPauseAlarmProvider implements Provider<JvmPauseAlarm> {
        private final JvmPauseAlarmConfig config;
        private final Lifecycle lifecycle;
        private Clock clock = Clock.systemUTC();

        @Inject
        JvmPauseAlarmProvider(JvmPauseAlarmConfig config, Lifecycle lifecycle) {
            this.config = config;
            this.lifecycle = lifecycle;
        }

        @Inject(optional = true)
        void setClock(Clock clock) {
            this.clock = clock;
        }

        @Override
        public JvmPauseAlarm get() {
            final JvmPauseAlarm alarm = new JvmPauseAlarm(clock::instant, config.getCheckTime().getMillis(), config.getPauseAlarmTime().getMillis(), l -> {});
            if (config.isPauseAlarmEnabled()) {
                lifecycle.addListener(LifecycleStage.START_STAGE, alarm::start);
                lifecycle.addListener(LifecycleStage.STOP_STAGE, alarm::close);
            }
            return alarm;
        }
    }
}

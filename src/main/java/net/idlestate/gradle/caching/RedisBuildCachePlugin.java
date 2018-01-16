/**
 * Copyright 2018 Thorsten Ehlers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.idlestate.gradle.caching;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.caching.configuration.BuildCacheConfiguration;

/**
 * Settings-Plugin that configures the remote build cache to use the Redis based implementation.
 * Without further settings the defaults are used. Those are 'localhost:6379' as Redis host
 * and a time to live for cached artifacts of 10 days.
 */
public class RedisBuildCachePlugin implements Plugin<Settings> {

    @Override
    public void apply( final Settings settings ) {
        final BuildCacheConfiguration buildCacheConfiguration = settings.getBuildCache();
        buildCacheConfiguration.registerBuildCacheService( RedisBuildCache.class, RedisBuildCacheServiceFactory.class );

        final RedisBuildCache cache = buildCacheConfiguration.remote( RedisBuildCache.class );
        cache.setPush( true );
    }
}

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

import org.gradle.caching.BuildCacheService;
import org.gradle.caching.BuildCacheServiceFactory;

/**
 * ServiceFactory that takes the given configuration to create a Redis based build-cache.
 */
public class RedisBuildCacheServiceFactory implements BuildCacheServiceFactory<RedisBuildCache> {

    @Override
    public BuildCacheService createBuildCacheService( final RedisBuildCache configuration, final Describer describer ) {
        describer
                .type( "redis" )
                .config( "host", configuration.getHost() )
                .config( "port", Integer.toString( configuration.getPort() ) )
                .config( "password", configuration.getPassword() == null ? "<without password>" : "<password given>" )
                .config( "ttl", Integer.toString( configuration.getTimeToLive() ) );

        return new RedisBuildCacheService( configuration.getHost(), configuration.getPort(), configuration.getPassword(), configuration.getTimeToLive() );
    }
}

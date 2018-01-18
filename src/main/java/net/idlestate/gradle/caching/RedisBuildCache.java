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

import org.gradle.caching.configuration.AbstractBuildCache;

/**
 * Configuration of the Redis based build cache.
 */
public class RedisBuildCache extends AbstractBuildCache {
    private static final int DEFAULT_PORT = 6379;
    private static final int DEFAULT_TIME_TO_LIVE = 10 * 24 * 60; // 10 days as minutes

    private String _host = System.getProperty( "net.idlestate.gradle.caching.redis.host", "localhost" );
    private int _port = getDefaultPort();
    private String _password = System.getProperty( "net.idlestate.gradle.caching.redis.password", null );

    private int _timeToLive = getDefaultTimeToLive();

    private static int getDefaultPort() {
        final String port = System.getProperty( "net.idlestate.gradle.caching.redis.port", Integer.toString( DEFAULT_PORT ) );

        try {
            return Integer.parseInt( port );
        } catch ( final NumberFormatException e ) {
            return DEFAULT_PORT;
        }
    }

    private int getDefaultTimeToLive() {
        final String timeToLive = System.getProperty( "net.idlestate.gradle.caching.redis.ttl", Integer.toString( DEFAULT_TIME_TO_LIVE ) );

        try {
            return Integer.parseInt( timeToLive );
        } catch ( final NumberFormatException e ) {
            return DEFAULT_TIME_TO_LIVE;
        }
    }

    /**
     * @return Redis host - default: localhost.
     */
    public String getHost() {
        return _host;
    }

    public void setHost( final String host ) {
        _host = host;
    }

    /**
     * @return Redis port - default: 6379.
     */
    public int getPort() {
        return _port;
    }

    public void setPort( final int port ) {
        _port = port;
    }


    /**
     * @return Redis password - default: no password.
     */
    public String getPassword() {
        return _password;
    }

    public void setPassword( final String password ) {
        _password = password;
    }

    /**
     * @return TIme to live of cached build artifacts in minutes - default: 14400 (10 days).
     */
    public int getTimeToLive() {
        return _timeToLive;
    }

    public void setTimeToLive( final int timeToLive ) {
        _timeToLive = timeToLive;
    }
}

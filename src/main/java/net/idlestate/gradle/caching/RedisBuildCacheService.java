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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.function.Function;

import org.gradle.caching.BuildCacheEntryReader;
import org.gradle.caching.BuildCacheEntryWriter;
import org.gradle.caching.BuildCacheException;
import org.gradle.caching.BuildCacheKey;
import org.gradle.caching.BuildCacheService;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisException;

/**
 * BuildCacheService that stores the build artifacts in Redis.
 * The configurable expiration date of the artifacts is refreshed on every
 * access.
 */
public class RedisBuildCacheService implements BuildCacheService {

    private static final String MISSES = "misses";
    private static final String HITS = "hits";
    private static final String BEGIN = "begin";
    private static final String SIZE = "size";
    private static final String END = "end";
    private static final String DURATION = "duration";
    private final JedisPool _jedisPool;
    private final int _timeToLive;

    RedisBuildCacheService( final String host, final int port, final String password, final int timeToLive ) {
        _jedisPool = new JedisPool( new JedisPoolConfig(), host, port, Protocol.DEFAULT_TIMEOUT, password );
        _timeToLive = timeToLive * 60;
    }

    @Override
    public boolean load( final BuildCacheKey key, final BuildCacheEntryReader reader ) throws BuildCacheException {
        final String dailyStatisticKey = createDailyStatisticKey();
        final String keyStatisticKey = createKeyStatisticKey( key );

        return withJedis( jedis -> {
            final String value = jedis.get( key.getHashCode() );
            if ( value != null ) {
                try {
                    reader.readFrom( new ByteArrayInputStream( Base64.getDecoder().decode( value ) ) );
                    jedis.expire( key.getHashCode(), _timeToLive );

                    jedis.hincrBy( dailyStatisticKey, HITS, 1 );
                    jedis.hincrBy( dailyStatisticKey, "key_" + key.getHashCode() + "_"+ HITS, 1 );
                    jedis.expire( dailyStatisticKey, _timeToLive );

                    jedis.hincrBy( keyStatisticKey, HITS, 1 );
                    jedis.expire( keyStatisticKey, _timeToLive );

                    return Boolean.TRUE;
                } catch ( final IOException e ) {
                    throw new BuildCacheException( "Unable to convert '" + value + "' to InputStream.", e );
                }
            }

            jedis.hincrBy( dailyStatisticKey, MISSES, 1 );
            jedis.hincrBy( dailyStatisticKey, "key_" + key.getHashCode() + "_" + MISSES, 1 );
            jedis.expire( dailyStatisticKey, _timeToLive );

            jedis.hincrBy( keyStatisticKey, MISSES, 1 );
            jedis.hset( keyStatisticKey, BEGIN, Long.toString( Instant.now().toEpochMilli() ) );
            jedis.expire( keyStatisticKey, _timeToLive );

            return Boolean.FALSE;
        } );
    }

    private static String createDailyStatisticKey() {
        return "statistic_" + LocalDate.now().format( DateTimeFormatter.ISO_DATE );
    }

    private static String createKeyStatisticKey( final BuildCacheKey key ) {
        return "key_statistic_" + key.getHashCode();
    }

    @Override
    public void store( final BuildCacheKey key, final BuildCacheEntryWriter writer ) throws BuildCacheException {
        final ByteArrayOutputStream value = new ByteArrayOutputStream();

        try {
            writer.writeTo( value );
        } catch ( final IOException e ) {
            throw new BuildCacheException( "Unable to read value to store.", e );
        } finally {
            try {
                value.close();
            } catch ( final IOException e ) {
                throw new BuildCacheException( "Unable to read value to store.", e );
            }
        }

        withJedis( jedis -> {
            final String encodedValue = Base64.getEncoder().encodeToString( value.toByteArray() );
            jedis.setex( key.getHashCode(), _timeToLive, encodedValue );

            final String statisticKey = createKeyStatisticKey( key );
            jedis.hset( statisticKey, SIZE, Integer.toString( encodedValue.length() ) );

            final long end = Instant.now().toEpochMilli();
            jedis.hset( statisticKey, END, Long.toString( end ) );

            final String beginString = jedis.hget( statisticKey, BEGIN );
            if ( beginString != null ) {
                try {
                    final long begin = Long.parseLong( beginString );
                    jedis.hset( statisticKey, DURATION, Long.toString( end - begin ) );
                } catch ( final NumberFormatException e ) {
                    // ignored
                }
            }
            jedis.expire( statisticKey, _timeToLive );

            return Boolean.TRUE;
        } );
    }

    @Override
    public void close() throws JedisException {
        _jedisPool.close();
    }

    private <T> T withJedis( final Function<Jedis, T> jedisOperation ) {
        Jedis jedis = null;

        try {
            jedis = _jedisPool.getResource();
            return jedisOperation.apply( jedis );
        } catch ( final Throwable e ) {
            throw new BuildCacheException( "Failed to communicate with Redis.", e );
        } finally {
            if ( jedis != null ) {
                try {
                    jedis.close();
                } catch ( final Throwable e ) {
                    throw new BuildCacheException( "Unable to close connection.", e );
                }
            }
        }
    }
}

package com.asadmshah.hnclone.cache;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;

public class BlockedSessionsCacheImplTest {

    private CacheImpl cache;
    private BlockedSessionsCacheImpl blockedCache;

    @Before
    public void setUp() throws Exception {
        Configuration configuration = new Configurations().properties(BlockedSessionsCacheImplTest.class.getClassLoader().getResource("test.properties"));

        cache = new CacheImpl(configuration);
        blockedCache = new BlockedSessionsCacheImpl(cache, 1, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() throws Exception {
        cache.stop();
    }

    @Test
    public void shouldContainSession() throws Exception {
        blockedCache.put(1);
        assertThat(blockedCache.contains(1, LocalDateTime.now().minusSeconds(1))).isTrue();
    }

    @Test
    public void shouldContainButIssuedAfter() throws Exception {
        blockedCache.put(1);
        assertThat(blockedCache.contains(1, LocalDateTime.now().plusSeconds(1))).isFalse();
    }

    @Test
    public void shouldNotContainBecauseOfEviction() throws Exception {
        blockedCache.put(1);
        Thread.sleep(1010);
        assertThat(blockedCache.contains(1, LocalDateTime.now().minusSeconds(1))).isFalse();
    }
}

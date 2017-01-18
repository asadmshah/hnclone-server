package com.asadmshah.hnclone.cache;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;

public class BlockedSessionsCacheImplTest {

    private BlockedSessionsCache cache;

    @Before
    public void setUp() throws Exception {
        cache = new BlockedSessionsCacheImpl(500, TimeUnit.MILLISECONDS);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void shouldContainSession() throws Exception {
        cache.put(1);
        assertThat(cache.contains(1, LocalDateTime.now().minusSeconds(1))).isTrue();
    }

    @Test
    public void shouldContainButIssuedAfter() throws Exception {
        cache.put(1);
        assertThat(cache.contains(1, LocalDateTime.now().plusSeconds(1))).isFalse();
    }

    @Test
    public void shouldNotContainBecauseOfEviction() throws Exception {
        cache.put(1);
        Thread.sleep(500);
        assertThat(cache.contains(1, LocalDateTime.now().minusSeconds(1))).isFalse();
    }
}

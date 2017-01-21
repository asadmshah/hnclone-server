package com.asadmshah.hnclone.cache;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;

import static com.google.common.truth.Truth.assertThat;

public class CacheImplTest {

    private CacheImpl cache;

    @Before
    public void setUp() throws Exception {
        Configuration configuration = new Configurations().properties(BlockedSessionsCacheImplTest.class.getClassLoader().getResource("test.properties"));

        cache = new CacheImpl(configuration);
    }

    @After
    public void tearDown() throws Exception {
        cache.stop();
    }

    @Test
    public void getPutLong_shouldComplete() {
        cache.put(Zone.BLOCKED_SESSIONS, "foo", 10L);
        assertThat(cache.getLong(Zone.BLOCKED_SESSIONS, "foo")).isEqualTo(10L);
    }

    @Test
    public void getPutLongExpires_shouldComplete() throws Exception {
        cache.put(Zone.BLOCKED_SESSIONS, "foo", 10L, LocalDateTime.now().plusSeconds(1));
        Thread.sleep(1100);
        assertThat(cache.getLong(Zone.BLOCKED_SESSIONS, "foo")).isNull();
    }

}

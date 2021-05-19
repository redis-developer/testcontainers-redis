package com.redislabs.testcontainers;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

@Testcontainers
public class TestRedisContainer {

    @Container
    protected static final RedisContainer REDIS = new RedisContainer();
    private RedisClient client;
    private StatefulRedisConnection<String, String> connection;

    @BeforeAll
    static void isRunning() {
        Assertions.assertTrue(REDIS.isRunning());
    }

    @BeforeEach
    public void setupEach() {
        this.client = RedisClient.create(REDIS.getRedisURI());
        this.connection = client.connect();
    }

    @AfterEach
    public void cleanupEach() {
        connection.sync().flushall();
        connection.close();
        client.shutdown();
        client.getResources().shutdown();
    }

    @Test
    void canPing() {
        Assertions.assertEquals("PONG", connection.sync().ping());
    }

    @Test
    void canWrite() {
        Map<String, String> hash = new HashMap<>();
        hash.put("field1", "value1");
        connection.sync().hset("hash:test", hash);
        Map<String, String> response = connection.sync().hgetall("hash:test");
        Assertions.assertEquals(hash, response);
    }

}

package com.redislabs.testcontainers;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

//@Testcontainers
@Slf4j
public class TestRedisEnterpriseContainer {

    private static final String ROOT_LOGGER = "";
    private RedisClient client;
    private StatefulRedisConnection<String, String> connection;

    @BeforeAll
    static void isRunning() {
        RedisEnterpriseContainer redisEnterprise = new RedisEnterpriseContainer();
        redisEnterprise.withLogConsumer(new Slf4jLogConsumer(log));
        redisEnterprise.start();
        Assertions.assertTrue(redisEnterprise.isRunning());
    }

//    @BeforeEach
//    public void setupEach() {
//        this.client = RedisClient.create(REDIS_ENTERPRISE.getRedisURI());
//        this.connection = client.connect();
//    }
//
//    @AfterEach
//    public void cleanupEach() {
//        connection.sync().flushall();
//        connection.close();
//        client.shutdown();
//    }

    @Test
    void canPing() {
//        Assertions.assertEquals("PONG", connection.sync().ping());
    }
//
//    @Test
//    void canWrite() {
//        Map<String, String> hash = new HashMap<>();
//        hash.put("field1", "value1");
//        connection.sync().hset("hash:test", hash);
//        Map<String, String> response = connection.sync().hgetall("hash:test");
//        Assertions.assertEquals(hash, response);
//    }

    public static void main(String[] args) {
        isRunning();
    }
}

package com.redislabs.testcontainers;

import io.lettuce.core.api.async.BaseRedisAsyncCommands;
import io.lettuce.core.api.reactive.BaseRedisReactiveCommands;
import io.lettuce.core.api.sync.BaseRedisCommands;
import io.lettuce.core.api.sync.RedisServerCommands;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.stream.Stream;

@Testcontainers
public class BaseRedisTest {

    @Container
    protected static final RedisContainer REDIS = new RedisContainer();

    @Container
    protected static final RedisContainer REDIS_CLUSTER = new RedisContainer().withClusterMode();

    @BeforeAll
    static void isRunning() {
        Assertions.assertTrue(REDIS.isRunning());
        Assertions.assertTrue(REDIS_CLUSTER.isRunning());
    }

    @AfterEach
    public void cleanupEach() {
        RedisServerCommands<String, String> redisCommands = REDIS.sync();
        redisCommands.flushall();
        RedisServerCommands<String, String> redisClusterCommands = REDIS_CLUSTER.sync();
        redisClusterCommands.flushall();
    }

    static Stream<RedisContainer> containers() {
        return Stream.of(REDIS, REDIS_CLUSTER);
    }

    static Stream<BaseRedisCommands<String, String>> sync() {
        return containers().map(RedisContainer::sync);
    }

    static Stream<BaseRedisAsyncCommands<String, String>> async() {
        return containers().map(RedisContainer::async);
    }

    static Stream<BaseRedisReactiveCommands<String, String>> reactive() {
        return containers().map(RedisContainer::reactive);
    }

}

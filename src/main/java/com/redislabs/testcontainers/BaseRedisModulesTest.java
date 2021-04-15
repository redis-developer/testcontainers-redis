package com.redislabs.testcontainers;

import com.redislabs.mesclun.RedisModulesAsyncCommands;
import com.redislabs.mesclun.RedisModulesCommands;
import com.redislabs.mesclun.RedisModulesReactiveCommands;
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
public class BaseRedisModulesTest {

    @Container
    protected static final RedisModulesContainer REDIS = new RedisModulesContainer();

    @BeforeAll
    static void isRunning() {
        Assertions.assertTrue(REDIS.isRunning());
    }

    @AfterEach
    public void cleanupEach() {
        RedisServerCommands<String, String> redisCommands = REDIS.sync();
        redisCommands.flushall();
    }

    static Stream<RedisModulesContainer> containers() {
        return Stream.of(REDIS);
    }

    static Stream<RedisModulesCommands<String, String>> sync() {
        return containers().map(RedisContainer::sync);
    }

    static Stream<RedisModulesAsyncCommands<String, String>> async() {
        return containers().map(RedisContainer::async);
    }

    static Stream<RedisModulesReactiveCommands<String, String>> reactive() {
        return containers().map(RedisContainer::reactive);
    }

}

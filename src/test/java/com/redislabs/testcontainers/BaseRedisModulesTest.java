package com.redislabs.testcontainers;

import com.redislabs.mesclun.RedisModulesClient;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
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

    static Stream<RedisModulesContainer> containers() {
        return Stream.of(REDIS);
    }

    @AfterEach
    public void cleanupEach() {
        RedisServerCommands<String, String> sync = sync(REDIS);
        sync.flushall();
    }

    @SuppressWarnings("unchecked")
    protected <T> T sync(RedisModulesContainer redisContainer) {
        return (T) RedisModulesClient.create(redisContainer.getRedisUri()).connect().sync();
    }

}

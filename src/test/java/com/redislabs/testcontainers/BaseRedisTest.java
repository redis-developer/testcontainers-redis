package com.redislabs.testcontainers;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisServerCommands;
import io.lettuce.core.cluster.RedisClusterClient;
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

    static Stream<RedisContainer> containers() {
        return Stream.of(REDIS, REDIS_CLUSTER);
    }

    @AfterEach
    public void cleanupEach() {
        RedisServerCommands<String, String> sync = sync(REDIS);
        sync.flushall();
        sync = sync(REDIS_CLUSTER);
        sync.flushall();
    }


    @SuppressWarnings("unchecked")
    protected <T> T sync(RedisContainer redisContainer) {
        if (redisContainer.isCluster()) {
            return (T) RedisClusterClient.create(redisContainer.getRedisUri()).connect().sync();
        }
        return (T) RedisClient.create(redisContainer.getRedisUri()).connect().sync();
    }

}

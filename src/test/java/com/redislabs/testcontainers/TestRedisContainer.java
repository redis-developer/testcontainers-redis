package com.redislabs.testcontainers;

import io.lettuce.core.api.sync.BaseRedisCommands;
import io.lettuce.core.api.sync.RedisHashCommands;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

@Testcontainers
public class TestRedisContainer extends BaseRedisTest {


    @ParameterizedTest(name = "can ping {0}")
    @MethodSource("containers")
    void canPing(RedisContainer redisContainer) {
         BaseRedisCommands<String,String> commands = redisContainer.sync();
        Assertions.assertEquals("PONG", commands.ping());
    }

    @ParameterizedTest(name = "can write to {0}")
    @MethodSource("containers")
    void canWrite(RedisContainer redisContainer) {
        RedisHashCommands<String, String> commands = redisContainer.sync();
        Map<String,String> hash = new HashMap<>();
        hash.put("field1", "value1");
        commands.hset("hash:test", hash);
        Map<String, String> response = commands.hgetall("hash:test");
        Assertions.assertEquals(hash, response);
    }

}

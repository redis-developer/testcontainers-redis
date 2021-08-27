package com.redis.testcontainers;

import com.redis.lettucemod.RedisModulesClient;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class TestRedisModulesImage {

    @Test
    void previewTag() {
        RedisModulesContainer container = new RedisModulesContainer("preview");
        container.start();
        RedisModulesClient client = RedisModulesClient.create(container.getRedisURI());
        StatefulRedisModulesConnection<String, String> connection = client.connect();
        Assertions.assertEquals("PONG", connection.sync().ping());
        connection.close();
        client.shutdown();
        client.getResources().shutdown();
        container.stop();
    }
}

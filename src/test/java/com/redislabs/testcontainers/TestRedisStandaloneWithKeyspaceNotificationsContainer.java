package com.redislabs.testcontainers;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;

@Testcontainers
public class TestRedisStandaloneWithKeyspaceNotificationsContainer {

    @Container
    protected static final RedisStandaloneContainer REDIS = new RedisStandaloneContainer().withKeyspaceNotifications();
    private RedisClient client;
    private StatefulRedisConnection<String, String> connection;
    private StatefulRedisPubSubConnection<String, String> pubSubConnection;

    @BeforeAll
    static void isRunning() {
        Assertions.assertTrue(REDIS.isRunning());
    }

    @BeforeEach
    public void setupEach() {
        this.client = RedisClient.create(REDIS.getRedisURI());
        this.connection = client.connect();
        this.pubSubConnection = client.connectPubSub();
    }

    @AfterEach
    public void cleanupEach() {
        connection.sync().flushall();
        connection.close();
        pubSubConnection.close();
        client.shutdown();
    }

    @Test
    void emitsKeyspaceNotifications() throws InterruptedException {
        List<String> messages = new ArrayList<>();
        pubSubConnection.addListener(new PubSubListener(messages));
        pubSubConnection.sync().psubscribe("__keyspace@0__:*");
        connection.sync().set("key1", "value");
        connection.sync().set("key2", "value");
        Thread.sleep(10);
        Assertions.assertEquals(2, messages.size());
    }

    private static class PubSubListener extends RedisPubSubAdapter<String, String> {

        private final List<String> messages;

        private PubSubListener(List<String> messages) {
            this.messages = messages;
        }

        @Override
        public void message(String channel, String message) {
            messages.add(message);
        }

        @Override
        public void message(String pattern, String channel, String message) {
            messages.add(message);
        }
    }


}

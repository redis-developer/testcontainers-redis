package com.redislabs.testcontainers;

import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.RedisClusterPubSubAdapter;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;

@Testcontainers
public class TestRedisClusterWithKeyspaceNotificationsContainer {

    @Container
    protected static final RedisClusterContainer REDIS_CLUSTER = new RedisClusterContainer().withKeyspaceNotifications();

    private RedisClusterClient client;
    private StatefulRedisClusterConnection<String, String> connection;
    private StatefulRedisClusterPubSubConnection<String, String> pubSubConnection;

    @BeforeAll
    static void isRunning() {
        Assertions.assertTrue(REDIS_CLUSTER.isRunning());
    }

    @BeforeEach
    public void setupEach() {
        this.client = RedisClusterClient.create(REDIS_CLUSTER.getRedisURI());
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
        pubSubConnection.setNodeMessagePropagation(true);
        pubSubConnection.sync().upstream().commands().psubscribe("__keyspace@0__:*");
        Thread.sleep(10);
        connection.sync().set("key1", "value");
        connection.sync().set("key2", "value");
        Thread.sleep(10);
        Assertions.assertEquals(2, messages.size());
    }

    private static class PubSubListener extends RedisClusterPubSubAdapter<String, String> {

        private final List<String> messages;

        private PubSubListener(List<String> messages) {
            this.messages = messages;
        }

        @Override
        public void message(RedisClusterNode node, String channel, String message) {
            messages.add(message);
        }

        @Override
        public void message(RedisClusterNode node, String pattern, String channel, String message) {
            messages.add(message);
        }
    }


}

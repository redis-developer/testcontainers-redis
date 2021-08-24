package com.redis.testcontainers;

import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.ClusterPartitionParser;
import io.lettuce.core.cluster.models.partitions.Partitions;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

@Testcontainers
public class TestRedisCluster {

    @Container
    protected static final RedisClusterContainer REDIS_CLUSTER = new RedisClusterContainer();
    private RedisClusterClient client;
    private StatefulRedisClusterConnection<String, String> connection;

    @BeforeAll
    static void isRunning() {
        Assertions.assertTrue(REDIS_CLUSTER.isRunning());
    }

    @BeforeEach
    public void setupEach() {
        this.client = RedisClusterClient.create(REDIS_CLUSTER.getRedisURI());
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

    @Test
    void canListNodes() {
        String nodes = connection.sync().clusterNodes();
        Partitions partitions = ClusterPartitionParser.parse(nodes);
        Assertions.assertEquals(REDIS_CLUSTER.getRedisURIs().length, partitions.size());
    }
}

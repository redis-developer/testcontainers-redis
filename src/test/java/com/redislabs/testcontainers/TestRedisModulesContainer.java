package com.redislabs.testcontainers;

import com.redislabs.mesclun.RedisModulesClient;
import com.redislabs.mesclun.api.StatefulRedisModulesConnection;
import com.redislabs.mesclun.api.sync.RedisTimeSeriesCommands;
import com.redislabs.mesclun.gears.output.ExecutionResults;
import com.redislabs.mesclun.timeseries.CreateOptions;
import com.redislabs.mesclun.timeseries.Label;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
@Testcontainers
public class TestRedisModulesContainer {


    @Container
    protected static final RedisModulesContainer REDIS = new RedisModulesContainer();
    private RedisModulesClient client;
    private StatefulRedisModulesConnection<String, String> connection;

    @BeforeAll
    static void isRunning() {
        Assertions.assertTrue(REDIS.isRunning());
    }

    @BeforeEach
    public void setupEach() {
        this.client = RedisModulesClient.create(REDIS.getRedisURI());
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
    void canWrite() {
        Map<String, String> hash = new HashMap<>();
        hash.put("field1", "value1");
        connection.sync().hset("hash:test", hash);
        Map<String, String> response = connection.sync().hgetall("hash:test");
        Assertions.assertEquals(hash, response);
    }

    @Test
    void canPing() {
        Assertions.assertEquals("PONG", connection.sync().ping());
    }

    @Test
    void canExecuteRedisGearsFunction() {
        ExecutionResults results = connection.sync().pyExecute("GB().run()");
        Assertions.assertTrue(results.isOk());
    }

    @Test
    void canWriteToRedisTimeSeries() {
        RedisTimeSeriesCommands<String, String> ts = connection.sync();
        ts.create("temperature:3:11", CreateOptions.builder().retentionTime(6000).build(), Label.of("sensor_id", "2"), Label.of("area_id", "32"));
        // TS.ADD temperature:3:11 1548149181 30
        Long add1 = ts.add("temperature:3:11", 1548149181, 30);
        Assertions.assertEquals(1548149181, add1);
    }

}

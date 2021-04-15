package com.redislabs.testcontainers;

import com.redislabs.mesclun.gears.RedisGearsCommands;
import com.redislabs.mesclun.gears.output.ExecutionResults;
import com.redislabs.mesclun.timeseries.CreateOptions;
import com.redislabs.mesclun.timeseries.Label;
import com.redislabs.mesclun.timeseries.RedisTimeSeriesCommands;
import io.lettuce.core.api.sync.BaseRedisCommands;
import io.lettuce.core.api.sync.RedisHashCommands;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

@Testcontainers
public class TestRedisModulesContainer extends BaseRedisModulesTest {


    @ParameterizedTest(name = "can ping {0}")
    @MethodSource("containers")
    void canPing(RedisModulesContainer redisContainer) {
        BaseRedisCommands<String, String> commands = redisContainer.sync();
        Assertions.assertEquals("PONG", commands.ping());
    }

    @ParameterizedTest(name = "can execute RedisGears function on {0}")
    @MethodSource("containers")
    void canExecuteRedisGearsFunction(RedisModulesContainer redisContainer) {
        RedisGearsCommands<String, String> gears = redisContainer.sync();
        ExecutionResults results = gears.pyExecute("GB().run()");
        Assertions.assertTrue(results.isOk());
    }

    @ParameterizedTest(name = "can write to RedisTimeSeries at {0}")
    @MethodSource("containers")
    void canWriteToRedisTimeSeries(RedisModulesContainer redisContainer) {
        RedisTimeSeriesCommands<String, String> ts = redisContainer.sync();
        ts.create("temperature:3:11", CreateOptions.builder().retentionTime(6000).build(), Label.of("sensor_id", "2"), Label.of("area_id", "32"));
        // TS.ADD temperature:3:11 1548149181 30
        Long add1 = ts.add("temperature:3:11", 1548149181, 30);
        Assertions.assertEquals(1548149181, add1);
    }

}

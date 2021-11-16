package com.redis.testcontainers.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.testcontainers.junit.jupiter.Container;

import com.redis.lettucemod.RedisModulesClient;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.lettucemod.api.sync.RedisTimeSeriesCommands;
import com.redis.lettucemod.api.timeseries.CreateOptions;
import com.redis.lettucemod.output.ExecutionResults;
import com.redis.testcontainers.RedisModulesContainer;
import com.redis.testcontainers.RedisServer;
import com.redis.testcontainers.junit.jupiter.AbstractTestcontainersRedisTestBase;
import com.redis.testcontainers.junit.jupiter.RedisTestContext;
import com.redis.testcontainers.junit.jupiter.RedisTestContextsSource;

class RedisModulesTests extends AbstractTestcontainersRedisTestBase {

	@Container
	static final RedisModulesContainer REDIS = new RedisModulesContainer();

	@Override
	protected Collection<RedisServer> servers() {
		return Arrays.asList(REDIS);
	}

	@ParameterizedTest
	@RedisTestContextsSource
	void canWrite(RedisTestContext context) {
		Map<String, String> hash = new HashMap<>();
		hash.put("field1", "value1");
		context.sync().hset("hash:test", hash);
		Map<String, String> response = context.sync().hgetall("hash:test");
		Assertions.assertEquals(hash, response);
	}

	@ParameterizedTest
	@RedisTestContextsSource
	void canPing(RedisTestContext context) {
		Assertions.assertEquals("PONG", context.sync().ping());
	}

	@ParameterizedTest
	@RedisTestContextsSource
	void canExecuteRedisGearsFunction(RedisTestContext context) {
		ExecutionResults results = context.sync().pyexecute("GB().run()");
		Assertions.assertTrue(results.isOk());
	}

	@ParameterizedTest
	@RedisTestContextsSource
	void canWriteToRedisTimeSeries(RedisTestContext context) {
		RedisTimeSeriesCommands<String, String> ts = context.sync();
		ts.create("temperature:3:11", CreateOptions.<String, String>builder().retentionTime(6000)
				.label("sensor_id", "2").label("area_id", "32").build());
		// TS.ADD temperature:3:11 1548149181 30
		Long add1 = ts.add("temperature:3:11", 1548149181, 30);
		Assertions.assertEquals(1548149181, add1);
	}

	@Test
	void previewTag() {
		try (RedisModulesContainer container = new RedisModulesContainer("preview")) {
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

}

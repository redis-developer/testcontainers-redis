package com.redis.testcontainers;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.testcontainers.lifecycle.Startable;

import com.redis.lettucemod.RedisModulesClient;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.lettucemod.api.sync.RedisModulesCommands;
import com.redis.lettucemod.cluster.RedisModulesClusterClient;
import com.redis.lettucemod.search.Field;
import com.redis.lettucemod.search.SearchResults;
import com.redis.lettucemod.timeseries.CreateOptions;
import com.redis.lettucemod.timeseries.Label;
import com.redis.lettucemod.timeseries.Sample;
import com.redis.lettucemod.util.RedisModulesUtils;

import io.lettuce.core.AbstractRedisClient;

@TestInstance(Lifecycle.PER_CLASS)
@SuppressWarnings("unchecked")
public abstract class AbstractRedisTestBase {

	private RedisServer redis;
	private AbstractRedisClient client;
	private StatefulRedisModulesConnection<String, String> connection;
	private RedisModulesCommands<String, String> commands;

	protected abstract RedisServer getRedisServer();

	@BeforeAll
	public void setup() {
		redis = getRedisServer();
		if (redis instanceof Startable) {
			((Startable) redis).start();
		}
		client = client(redis);
		connection = RedisModulesUtils.connection(client);
		commands = connection.sync();
	}

	private AbstractRedisClient client(RedisServer redis) {
		if (redis.isRedisCluster()) {
			return RedisModulesClusterClient.create(redis.getRedisURI());
		}
		return RedisModulesClient.create(redis.getRedisURI());
	}

	@AfterAll
	public void teardown() {
		commands = null;
		if (connection != null) {
			connection.close();
		}
		if (client != null) {
			client.close();
		}
		if (redis instanceof Startable) {
			((Startable) redis).stop();
		}
	}

	@BeforeEach
	void flushall() {
		commands.flushall();
	}

	@Test
	void ping() {
		Assertions.assertEquals("PONG", commands.ping());
	}

	@Test
	void search() {
		commands.ftCreate("test", Field.text("name").build(), Field.tag("id").build());
		int count = 10;
		for (int index = 0; index < count; index++) {
			Map<String, String> doc = new HashMap<>();
			doc.put("name", "name " + index);
			doc.put("id", String.valueOf(index + 1));
			commands.hset("hash:" + index, doc);
		}
		SearchResults<String, String> results = commands.ftSearch("test", "*");
		Assertions.assertEquals(count, results.getCount());
	}

	@Test
	void timeseries() {
		// TimeSeries tests
		commands.tsCreate("temperature:3:11", CreateOptions.<String, String>builder().retentionPeriod(6000)
				.labels(Label.of("sensor_id", "2"), Label.of("area_id", "32")).build());
		// TS.ADD temperature:3:11 1548149181 30
		Long add1 = commands.tsAdd("temperature:3:11", Sample.of(1548149181, 30));
		Assertions.assertEquals(1548149181, add1);
	}

	@Test
	void writeHash() {
		// Write test
		Map<String, String> map = new HashMap<>();
		map.put("field1", "value1");
		String key = "testhash";
		commands.hset(key, map);
		Assertions.assertEquals(map, commands.hgetall(key));
	}

}

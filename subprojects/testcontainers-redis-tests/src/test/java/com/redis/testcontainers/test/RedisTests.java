package com.redis.testcontainers.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.testcontainers.junit.jupiter.Container;

import com.redis.testcontainers.RedisClusterContainer;
import com.redis.testcontainers.RedisContainer;
import com.redis.testcontainers.RedisServer;
import com.redis.testcontainers.junit.jupiter.AbstractTestcontainersRedisTestBase;
import com.redis.testcontainers.junit.jupiter.RedisTestContext;
import com.redis.testcontainers.junit.jupiter.RedisTestContextsSource;

import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.RedisClusterPubSubAdapter;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

class RedisTests extends AbstractTestcontainersRedisTestBase {

	@Container
	static final RedisContainer REDIS = new RedisContainer().withKeyspaceNotifications();
	@Container
	static final RedisClusterContainer REDIS_CLUSTER = new RedisClusterContainer().withKeyspaceNotifications();

	@Override
	protected Collection<RedisServer> servers() {
		return Arrays.asList(REDIS);
	}

	@ParameterizedTest
	@RedisTestContextsSource
	void canPing(RedisTestContext context) {
		Assertions.assertEquals("PONG", context.sync().ping());
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
	void emitsKeyspaceNotifications(RedisTestContext context) throws InterruptedException {
		String keyPattern = "__keyspace@0__:*";
		List<String> messages = new ArrayList<>();
		StatefulRedisPubSubConnection<String, String> pubSubConnection = context.getPubSubConnection();
		if (pubSubConnection instanceof StatefulRedisClusterPubSubConnection) {
			StatefulRedisClusterPubSubConnection<String, String> clusterPubSubConnection = (StatefulRedisClusterPubSubConnection<String, String>) pubSubConnection;
			clusterPubSubConnection.addListener(new ClusterPubSubListener(messages));
			clusterPubSubConnection.setNodeMessagePropagation(true);
			clusterPubSubConnection.sync().upstream().commands().psubscribe(keyPattern);

		} else {
			pubSubConnection.addListener(new PubSubListener(messages));
			pubSubConnection.sync().psubscribe(keyPattern);
		}
		Thread.sleep(10);
		context.sync().set("key1", "value");
		context.sync().set("key2", "value");
		Thread.sleep(10);
		Assertions.assertEquals(2, messages.size());
	}

	private static class ClusterPubSubListener extends RedisClusterPubSubAdapter<String, String> {

		private final List<String> messages;

		ClusterPubSubListener(List<String> messages) {
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

	private static class PubSubListener extends RedisPubSubAdapter<String, String> {

		private final List<String> messages;

		PubSubListener(List<String> messages) {
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

package com.redis.testcontainers;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.RedisClusterPubSubAdapter;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;

@Testcontainers
@DisabledOnOs(value = OS.MAC)
class RedisClusterContainerTests {

	@SuppressWarnings("resource")
	@Test
	void emitsKeyspaceNotifications() throws Exception {
		try (RedisClusterContainer redisCluster = new RedisClusterContainer(
				RedisClusterContainer.DEFAULT_IMAGE_NAME.withTag(RedisClusterContainer.DEFAULT_TAG))
				.withKeyspaceNotifications()) {
			Assumptions.assumeTrue(redisCluster.isEnabled());
			redisCluster.start();
			List<String> messages = new ArrayList<>();
			RedisClusterClient client = RedisClusterClient.create(redisCluster.getRedisURI());
			try (StatefulRedisClusterConnection<String, String> connection = client.connect();
					StatefulRedisClusterPubSubConnection<String, String> pubSubConnection = client.connectPubSub()) {
				pubSubConnection.addListener(new ClusterPubSubListener(messages));
				pubSubConnection.setNodeMessagePropagation(true);
				pubSubConnection.sync().upstream().commands().psubscribe("__keyspace@0__:*");
				connection.sync().set("key1", "value");
				connection.sync().set("key2", "value");
				Awaitility.await().until(() -> messages.size() == 2);
			} finally {
				client.shutdown();
				client.getResources().shutdown();
			}
		}
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

}

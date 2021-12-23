package com.redis.testcontainers;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.RedisClusterPubSubAdapter;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;

@Testcontainers
class RedisClusterContainerTests {

	@Container
	private static final RedisClusterContainer REDIS = new RedisClusterContainer(
			RedisClusterContainer.DEFAULT_IMAGE_NAME.withTag(RedisClusterContainer.DEFAULT_TAG))
					.withKeyspaceNotifications();

	@Test
	void emitsKeyspaceNotifications() throws InterruptedException {
		String keyPattern = "__keyspace@0__:*";
		List<String> messages = new ArrayList<>();
		RedisClusterClient client = RedisClusterClient.create(REDIS.getRedisURI());
		try (StatefulRedisClusterConnection<String, String> connection = client.connect();
				StatefulRedisClusterPubSubConnection<String, String> pubSubConnection = client.connectPubSub()) {
			pubSubConnection.addListener(new ClusterPubSubListener(messages));
			pubSubConnection.setNodeMessagePropagation(true);
			pubSubConnection.sync().upstream().commands().psubscribe(keyPattern);
			Thread.sleep(10);
			connection.sync().set("key1", "value");
			connection.sync().set("key2", "value");
			Thread.sleep(10);
			Assertions.assertEquals(2, messages.size());
		}
		client.shutdown();
		client.getResources().shutdown();
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

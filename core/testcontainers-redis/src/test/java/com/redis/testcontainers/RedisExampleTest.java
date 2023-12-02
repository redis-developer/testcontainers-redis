package com.redis.testcontainers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

@Testcontainers
class RedisExampleTest {

	@Container
	private static RedisContainer container = new RedisContainer(
			RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG));

	@Test
	void testSomethingUsingLettuce() {
		// Retrieve the Redis URI from the container
		String redisURI = container.getRedisURI();
		RedisClient client = RedisClient.create(redisURI);
		try (StatefulRedisConnection<String, String> connection = client.connect()) {
			RedisCommands<String, String> commands = connection.sync();
			Assertions.assertEquals("PONG", commands.ping());
		}
	}
}

package com.redis.testcontainers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.redis.lettucemod.RedisModulesClient;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.lettucemod.api.sync.RedisModulesCommands;

@Testcontainers
public class RedisStackExampleTest {

	@Container
	private static RedisStackContainer redisContainer = new RedisStackContainer(
			RedisStackContainer.DEFAULT_IMAGE_NAME.withTag(RedisStackContainer.DEFAULT_TAG));

	@Test
	void testSomethingUsingLettuce() {
		// Retrieve the Redis URI from the container
		String redisURI = redisContainer.getRedisURI();
		RedisModulesClient client = RedisModulesClient.create(redisURI);
		try (StatefulRedisModulesConnection<String, String> connection = client.connect()) {
			RedisModulesCommands<String, String> commands = connection.sync();
			Assertions.assertEquals("PONG", commands.ping());
		}
	}
}

package com.redis.testcontainers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.redis.lettucemod.RedisModulesClient;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;

@Testcontainers
class RedisModulesImageTests {

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

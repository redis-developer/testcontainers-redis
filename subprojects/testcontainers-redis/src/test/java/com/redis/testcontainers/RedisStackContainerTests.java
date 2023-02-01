package com.redis.testcontainers;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.redis.lettucemod.RedisModulesClient;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.lettucemod.search.Field;
import com.redis.lettucemod.search.SearchResults;

@Testcontainers
@SuppressWarnings("unchecked")
class RedisStackContainerTests {

	@Test
	void startContainer() throws InterruptedException {
		try (RedisStackContainer redis = new RedisStackContainer(
				RedisStackContainer.DEFAULT_IMAGE_NAME.withTag(RedisStackContainer.DEFAULT_TAG))) {
			redis.start();
			assertConnectAndSearch(redis);
		}
	}

	@Test
	void startContainerWithEnv() throws InterruptedException {
		try (RedisStackContainer redis = new RedisStackContainer(
				RedisStackContainer.DEFAULT_IMAGE_NAME.withTag(RedisStackContainer.DEFAULT_TAG))
				.withEnv("REDISEARCH_ARGS", "MAXAGGREGATERESULTS 100000")) {
			redis.start();
			assertConnectAndSearch(redis);
		}
	}

	private void assertConnectAndSearch(RedisServer redis) {
		RedisModulesClient client = RedisModulesClient.create(redis.getRedisURI());
		try (StatefulRedisModulesConnection<String, String> connection = client.connect()) {
			connection.sync().ftCreate("test", Field.text("name").build(), Field.tag("id").build());
			for (int index = 0; index < 10; index++) {
				Map<String, String> hash = new HashMap<>();
				hash.put("name", "name " + index);
				hash.put("id", String.valueOf(index + 1));
				connection.sync().hset("hash:" + index, hash);
			}
			SearchResults<String, String> results = connection.sync().ftSearch("test", "*");
			Assertions.assertEquals(10, results.getCount());
		} finally {
			client.shutdown();
			client.getResources().shutdown();
		}
	}

}

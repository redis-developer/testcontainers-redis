package com.redis.testcontainers.junit;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;

import com.redis.testcontainers.RedisClusterContainer;
import com.redis.testcontainers.RedisContainer;
import com.redis.testcontainers.RedisEnterpriseContainer;
import com.redis.testcontainers.RedisModulesContainer;
import com.redis.testcontainers.RedisServer;

class RedisTests extends AbstractTestcontainersRedisTestBase {

	/**
	 * 
	 * @return Redis containers required for the tests
	 */
	@SuppressWarnings("resource")
	@Override
	protected Collection<RedisServer> redisServers() {
		return Arrays.asList(
				new RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG))
						.withKeyspaceNotifications(),
				new RedisModulesContainer(
						RedisModulesContainer.DEFAULT_IMAGE_NAME.withTag(RedisModulesContainer.DEFAULT_TAG)),
				new RedisModulesContainer(RedisModulesContainer.DEFAULT_IMAGE_NAME.withTag("preview")),
				new RedisClusterContainer(
						RedisClusterContainer.DEFAULT_IMAGE_NAME.withTag(RedisClusterContainer.DEFAULT_TAG))
								.withKeyspaceNotifications(),
				new RedisEnterpriseContainer(
						RedisEnterpriseContainer.DEFAULT_IMAGE_NAME.withTag(RedisEnterpriseContainer.DEFAULT_TAG)));
	}

	/**
	 * Assert that the Redis server can be pinged
	 * 
	 * @param context test context, including the Redis server to test against
	 */
	@ParameterizedTest
	@RedisTestContextsSource
	void canPing(RedisTestContext context) {
		Assertions.assertEquals("PONG", context.sync().ping());
	}

	/**
	 * Assert that the Redis server can be written to
	 * 
	 * @param context test context, including the Redis server to test against
	 */
	@ParameterizedTest
	@RedisTestContextsSource
	void canWrite(RedisTestContext context) {
		Map<String, String> hash = new HashMap<>();
		hash.put("field1", "value1");
		context.sync().hset("hash:test", hash);
		Map<String, String> response = context.sync().hgetall("hash:test");
		Assertions.assertEquals(hash, response);
	}

}

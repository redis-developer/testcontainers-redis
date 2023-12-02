package com.redis.testcontainers;

import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class RedisStackContainerWithEnvTests extends AbstractModulesTestBase {

	private static final RedisStackContainer container = new RedisStackContainer(
			RedisStackContainer.DEFAULT_IMAGE_NAME.withTag(RedisStackContainer.DEFAULT_TAG))
			.withEnv("REDISEARCH_ARGS", "MAXAGGREGATERESULTS 100000");

	@Override
	protected RedisStackContainer getRedisContainer() {
		return container;
	}
}

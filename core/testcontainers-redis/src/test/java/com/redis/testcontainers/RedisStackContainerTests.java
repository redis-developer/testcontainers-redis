package com.redis.testcontainers;

class RedisStackContainerTests extends AbstractTestBase {

	private static final RedisStackContainer container = new RedisStackContainer(
			RedisStackContainer.DEFAULT_IMAGE_NAME.withTag(RedisStackContainer.DEFAULT_TAG));

	@Override
	protected RedisStackContainer getRedisServer() {
		return container;
	}

}

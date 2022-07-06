package com.redis.testcontainers;

import org.testcontainers.utility.DockerImageName;

public class RedisStackContainer extends AbstractRedisContainer<RedisStackContainer> {

	public static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("redis/redis-stack");
	public static final String DEFAULT_TAG = "latest";
	public static final String ENV_ENABLED_SUFFIX = "REDIS_STACK";

	public RedisStackContainer(final DockerImageName dockerImageName) {
		super(dockerImageName);
	}

	@Override
	public boolean isCluster() {
		return false;
	}

	@Override
	public boolean isEnabled() {
		return RedisServer.isEnabled(ENV_ENABLED_SUFFIX);
	}

}

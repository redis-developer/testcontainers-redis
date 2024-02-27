package com.redis.testcontainers;

import org.testcontainers.utility.DockerImageName;

public class RedisStackContainer extends AbstractRedisContainer<RedisStackContainer> {

	public static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("redis/redis-stack-server");

	public static final String DEFAULT_TAG = "latest";

	public RedisStackContainer(String dockerImageName) {
		super(dockerImageName);
	}

	public RedisStackContainer(final DockerImageName dockerImageName) {
		super(dockerImageName);
	}

}

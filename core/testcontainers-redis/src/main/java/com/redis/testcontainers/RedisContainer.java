package com.redis.testcontainers;

import org.testcontainers.utility.DockerImageName;

public class RedisContainer extends AbstractRedisServerContainer<RedisContainer> {

	public static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("redis");
	
	public static final String DEFAULT_TAG = "latest";

	public RedisContainer(String dockerImageName) {
		super(dockerImageName);
	}

	public RedisContainer(final DockerImageName dockerImageName) {
		super(dockerImageName);
	}

}

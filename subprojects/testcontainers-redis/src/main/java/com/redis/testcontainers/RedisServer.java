package com.redis.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startable;

public interface RedisServer extends Startable {

	String getRedisURI();

	boolean isCluster();

	boolean isActive();

	static String redisURI(String host, int port) {
		return "redis://" + host + ":" + port;
	}

	static String redisURI(GenericContainer<?> container) {
		return redisURI(container.getHost(), container.getFirstMappedPort());
	}

}

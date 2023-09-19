package com.redis.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.shaded.org.apache.commons.lang3.ClassUtils;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;

public interface RedisServer extends Startable {

	String getRedisURI();

	boolean isCluster();

	boolean isEnabled();

	static String redisURI(String host, int port) {
		return "redis://" + host + ":" + port;
	}

	static String redisURI(GenericContainer<?> container) {
		return redisURI(container.getHost(), container.getFirstMappedPort());
	}

	static String toString(RedisServer server) {
		return ClassUtils.getShortClassName(server.getClass());
	}

	static boolean isEnabled(String suffix) {
		String name = "TESTCONTAINERS_" + suffix;
		String value = System.getProperty(name, System.getenv(name));
		// Containers are enabled by default
		if (StringUtils.isEmpty(value)) {
			return true;
		}
		return !value.toLowerCase().matches("disabled|off|false|no");
	}

}

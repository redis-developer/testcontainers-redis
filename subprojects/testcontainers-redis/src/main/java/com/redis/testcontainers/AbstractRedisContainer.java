package com.redis.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.org.apache.commons.lang.ClassUtils;
import org.testcontainers.utility.DockerImageName;

abstract class AbstractRedisContainer<C extends AbstractRedisContainer<C>> extends GenericContainer<C>
		implements RedisServer {

	public static final int REDIS_PORT = 6379;

	protected AbstractRedisContainer(final DockerImageName dockerImageName) {
		this(dockerImageName, REDIS_PORT);
	}

	protected AbstractRedisContainer(final DockerImageName dockerImageName, int port) {
		super(dockerImageName);
		withExposedPorts(port);
		waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));
	}

	/**
	 * Get Redis URI.
	 *
	 * @return Redis URI.
	 */
	@Override
	public String getRedisURI() {
		return RedisServer.redisURI(this);
	}

	@Override
	public String toString() {
		return ClassUtils.getShortClassName(getClass()) + " active=" + isActive();
	}

}

package com.redis.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.org.apache.commons.lang3.ClassUtils;
import org.testcontainers.utility.DockerImageName;

public abstract class AbstractRedisContainer<C extends AbstractRedisContainer<C>> extends GenericContainer<C> {

	protected AbstractRedisContainer(final DockerImageName dockerImageName) {
		super(dockerImageName);
	}

	/**
	 * Get Redis URI.
	 *
	 * @return Redis URI.
	 */
	public abstract String getRedisURI();
	
	public abstract boolean isCluster();
	
	protected String redisURI(String host, int port) {
		return "redis://" + host + ":" + port;
	}

	@Override
	public String toString() {
		return ClassUtils.getShortClassName(getClass());
	}

}

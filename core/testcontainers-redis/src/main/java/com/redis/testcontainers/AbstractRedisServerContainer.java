package com.redis.testcontainers;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public abstract class AbstractRedisServerContainer<C extends AbstractRedisServerContainer<C>> extends AbstractRedisContainer<C> {

	public static final int REDIS_PORT = 6379;

	protected AbstractRedisServerContainer(String dockerImageName) {
		this(DockerImageName.parse(dockerImageName));
	}

	protected AbstractRedisServerContainer(final DockerImageName dockerImageName) {
		super(dockerImageName);
		addExposedPorts(REDIS_PORT);
		waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));
	}

	@SuppressWarnings("unchecked")
	public C withKeyspaceNotifications() {
		withCopyFileToContainer(MountableFile.forClasspathResource("redis-keyspace-notifications.conf"),
				"/data/redis.conf");
		withCommand("redis-server", "/data/redis.conf");
		return (C) this;
	}

	/**
	 * Get Redis URI.
	 *
	 * @return Redis URI.
	 */
	@Override
	public String getRedisURI() {
		return redisURI(getHost(), getFirstMappedPort());
	}

	@Override
	public boolean isCluster() {
		return false;
	}

}

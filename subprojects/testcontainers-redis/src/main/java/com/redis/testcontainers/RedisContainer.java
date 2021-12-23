package com.redis.testcontainers;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class RedisContainer extends AbstractRedisContainer<RedisContainer> {

	public static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("redis");
	public static final String DEFAULT_TAG = "6.2.6";

	/**
	 * @deprecated use {@link RedisContainer(DockerImageName)} instead
	 */
	@Deprecated
	public RedisContainer() {
		this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
	}

	public RedisContainer(final DockerImageName dockerImageName) {
		super(dockerImageName);
	}

	/**
	 * @deprecated use {@link RedisContainer(DockerImageName)} instead
	 */
	@Deprecated
	public RedisContainer(final String tag) {
		this(DEFAULT_IMAGE_NAME.withTag(tag));
	}

	@Override
	public boolean isCluster() {
		return false;
	}

	@SuppressWarnings("unchecked")
	public <C extends RedisContainer> C withKeyspaceNotifications() {
		withCopyFileToContainer(MountableFile.forClasspathResource("redis-keyspace-notifications.conf"),
				"/data/redis.conf");
		withCommand("redis-server", "/data/redis.conf");
		return (C) this;
	}

}

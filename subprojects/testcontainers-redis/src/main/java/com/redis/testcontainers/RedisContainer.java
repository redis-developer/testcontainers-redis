package com.redis.testcontainers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class RedisContainer extends AbstractRedisContainer<RedisContainer> {

	private static final Logger log = LoggerFactory.getLogger(RedisContainer.class);

	public static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("redis");
	public static final String DEFAULT_TAG = "6.2.6";
	public static final String ENV_SKIP_TESTS = "skipRedisTests";

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

	@Override
	public boolean isActive() {
		String skipValue = System.getenv(ENV_SKIP_TESTS);
		boolean active = !Boolean.parseBoolean(skipValue);
		log.info("Active: {} ({}='{}'}", active, ENV_SKIP_TESTS, skipValue);
		return active;
	}

}

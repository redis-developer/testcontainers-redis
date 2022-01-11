package com.redis.testcontainers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.utility.DockerImageName;

public class RedisModulesContainer extends AbstractRedisContainer<RedisModulesContainer> {

	private static final Logger log = LoggerFactory.getLogger(RedisModulesContainer.class);

	public static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("redislabs/redismod");
	public static final String DEFAULT_TAG = "latest";
	public static final String ENV_SKIP_TESTS = "skipRedisModulesTests";

	/**
	 * @deprecated use {@link RedisModulesContainer(DockerImageName)} instead
	 */
	@Deprecated
	public RedisModulesContainer() {
		this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
	}

	/**
	 * @deprecated use {@link RedisModulesContainer(DockerImageName)} instead
	 */
	@Deprecated
	public RedisModulesContainer(final String tag) {
		this(DEFAULT_IMAGE_NAME.withTag(tag));
	}

	public RedisModulesContainer(final DockerImageName dockerImageName) {
		super(dockerImageName);
	}

	@Override
	public boolean isCluster() {
		return false;
	}

	@Override
	public boolean isActive() {
		String skipValue = System.getenv(ENV_SKIP_TESTS);
		boolean active = !Boolean.parseBoolean(skipValue);
		log.info("Active: {} ({}='{}'}", active, ENV_SKIP_TESTS, skipValue);
		return active;
	}

}

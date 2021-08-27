package com.redis.testcontainers;

import com.redis.testcontainers.support.AbstractRedisContainer;
import org.testcontainers.utility.DockerImageName;

public class RedisModulesContainer extends AbstractRedisContainer<RedisModulesContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("redislabs/redismod");
    private static final String DEFAULT_TAG = "latest";

    public RedisModulesContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public RedisModulesContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    public RedisModulesContainer(final String tag) {
        this(DEFAULT_IMAGE_NAME.withTag(tag));
    }

    @Override
    public boolean isCluster() {
        return false;
    }

}

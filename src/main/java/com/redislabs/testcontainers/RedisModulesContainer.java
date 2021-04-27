package com.redislabs.testcontainers;

import org.testcontainers.utility.DockerImageName;

public class RedisModulesContainer extends AbstractRedisContainer<RedisModulesContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("redislabs/redismod");
    private static final String DEFAULT_TAG = "latest";

    public RedisModulesContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    @Override
    public boolean isCluster() {
        return false;
    }

    protected RedisModulesContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
    }

}

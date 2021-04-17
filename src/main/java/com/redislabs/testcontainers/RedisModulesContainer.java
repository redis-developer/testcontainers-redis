package com.redislabs.testcontainers;

import org.testcontainers.utility.DockerImageName;

public class RedisModulesContainer extends RedisContainer {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("redislabs/redismod");
    private static final String DEFAULT_TAG = "latest";

    public RedisModulesContainer() {
        super(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public RedisModulesContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    @Override
    protected DockerImageName defaultImageName() {
        return DEFAULT_IMAGE_NAME;
    }

    @Override
    public <C extends RedisContainer> C withKeyspaceNotifications() {
        throw new UnsupportedOperationException("RedisModulesContainer does not support keyspace notifications");
    }

    @Override
    public <C extends RedisContainer> C withClusterKeyspaceNotifications() {
        throw new UnsupportedOperationException("RedisModulesContainer does not support keyspace notifications");
    }

    @Override
    public <C extends RedisContainer> C withClusterMode() {
        throw new UnsupportedOperationException("RedisModulesContainer does not support cluster mode");
    }
}

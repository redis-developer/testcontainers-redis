package com.redislabs.testcontainers;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class RedisStandaloneContainer extends AbstractRedisContainer<RedisStandaloneContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("redis");
    private static final String DEFAULT_TAG = "latest";

    public RedisStandaloneContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    protected RedisStandaloneContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    @Override
    public boolean isCluster() {
        return false;
    }

    @SuppressWarnings("unchecked")
    public <C extends RedisStandaloneContainer> C withKeyspaceNotifications() {
        withCopyFileToContainer(MountableFile.forClasspathResource("redis-keyspace-notifications.conf"), "/data/redis.conf");
        withCommand("redis-server", "/data/redis.conf");
        return (C) this;
    }

}

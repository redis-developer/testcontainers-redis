package com.redislabs.testcontainers;

import com.redislabs.testcontainers.support.AbstractRedisContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class RedisContainer extends AbstractRedisContainer<RedisContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("redis");
    private static final String DEFAULT_TAG = "latest";

    public RedisContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    protected RedisContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    @Override
    public boolean isCluster() {
        return false;
    }

    @SuppressWarnings("unchecked")
    public <C extends RedisContainer> C withKeyspaceNotifications() {
        withCopyFileToContainer(MountableFile.forClasspathResource("redis-keyspace-notifications.conf"), "/data/redis.conf");
        withCommand("redis-server", "/data/redis.conf");
        return (C) this;
    }

}

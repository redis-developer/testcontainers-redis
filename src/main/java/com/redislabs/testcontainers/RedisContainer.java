package com.redislabs.testcontainers;

import lombok.Getter;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * This container wraps Redis and optionally Redis Cluster
 * Some code borrowed from https://github.com/jaredpetersen/kafka-connect-redis
 */
public class RedisContainer extends GenericContainer<RedisContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("redis");
    private static final String DEFAULT_TAG = "6";

    public static final int REDIS_PORT = 6379;

    @Getter
    private boolean cluster;

    public RedisContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    protected RedisContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(defaultImageName());
        withExposedPorts(REDIS_PORT);
        waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));
    }

    protected DockerImageName defaultImageName() {
        return DEFAULT_IMAGE_NAME;
    }

    public <C extends RedisContainer> C withKeyspaceNotifications() {
        withCopyFileToContainer(MountableFile.forClasspathResource("redis-keyspace-notifications.conf"), "/data/redis.conf");
        withCommand("redis-server", "/data/redis.conf");
        return (C) this;
    }

    public <C extends RedisContainer> C withClusterKeyspaceNotifications() {
        return withClusterMode("redis-cluster-keyspace-notifications.conf");
    }

    /**
     * Enables Redis cluster mode
     *
     * @return a Redis Cluster container
     */
    public <C extends RedisContainer> C withClusterMode() {
        return withClusterMode("redis-cluster.conf");
    }

    private <C extends RedisContainer> C withClusterMode(String conf) {
        withCopyFileToContainer(MountableFile.forClasspathResource(conf), "/data/redis-cluster.conf");
        withCopyFileToContainer(MountableFile.forClasspathResource("nodes-cluster.conf"), "/data/nodes.conf");
        withCommand("redis-server", "/data/redis-cluster.conf");
        waitingFor(Wait.forLogMessage(".*Cluster state changed: ok*\\n", 1));
        cluster = true;
        return (C) this;
    }

    /**
     * Get Redis URI.
     *
     * @return Redis URI.
     */
    public String getRedisUri() {
        return "redis://" + this.getHost() + ":" + this.getFirstMappedPort();
    }

    @Override
    public String toString() {
        if (cluster) {
            return "cluster " + getRedisUri();
        }
        return getRedisUri();
    }

}

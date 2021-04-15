package com.redislabs.testcontainers;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.BaseRedisAsyncCommands;
import io.lettuce.core.api.reactive.BaseRedisReactiveCommands;
import io.lettuce.core.api.sync.BaseRedisCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * This container wraps Redis and optionally Redis Cluster
 * Some code borrowed from https://github.com/jaredpetersen/kafka-connect-redis
 */
public class RedisContainer extends GenericContainer<RedisContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("redis");
    private static final String DEFAULT_TAG = "6";

    public static final int REDIS_PORT = 6379;

    private boolean cluster;

    public RedisContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public RedisContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        withExposedPorts(REDIS_PORT);
        waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));
    }

    /**
     * Enables Redis cluster mode
     *
     * @return a Redis Cluster container
     */
    public RedisContainer withClusterMode() {
        withCopyFileToContainer(MountableFile.forClasspathResource("redis-cluster.conf"), "/data/redis.conf");
        withCopyFileToContainer(MountableFile.forClasspathResource("nodes-cluster.conf"), "/data/nodes.conf");
        withCommand("redis-server", "/data/redis.conf");
        waitingFor(Wait.forLogMessage(".*Cluster state changed: ok*\\n", 1));
        cluster = true;
        return this;
    }

    /**
     * Get Redis URI.
     *
     * @return Redis URI.
     */
    public String redisUri() {
        return "redis://" + this.getHost() + ":" + this.getFirstMappedPort();
    }

    public StatefulConnection<String, String> connection() {
        return connectionSupplier().get();
    }

    public Supplier<StatefulConnection<String, String>> connectionSupplier() {
        if (cluster) {
            RedisClusterClient client = RedisClusterClient.create(redisUri());
            return () -> client.connect();
        }
        RedisClient client = RedisClient.create(redisUri());
        return () -> client.connect();
    }

    @SuppressWarnings("unchecked")
    public <T> T sync() {
        return (T) syncFunction().apply(connection());
    }

    @SuppressWarnings("unchecked")
    public <T> T async() {
        return (T) asyncFunction().apply(connection());
    }

    @SuppressWarnings("unchecked")
    public <T> T reactive() {
        return (T) reactiveFunction().apply(connection());
    }

    public Function<StatefulConnection<String, String>, BaseRedisCommands<String, String>> syncFunction() {
        if (cluster) {
            return c -> ((StatefulRedisClusterConnection<String, String>) c).sync();
        }
        return c -> ((StatefulRedisConnection<String, String>) c).sync();
    }

    public Function<StatefulConnection<String, String>, BaseRedisAsyncCommands<String, String>> asyncFunction() {
        if (cluster) {
            return c -> ((StatefulRedisClusterConnection<String, String>) c).async();
        }
        return c -> ((StatefulRedisConnection<String, String>) c).async();
    }

    public Function<StatefulConnection<String, String>, BaseRedisReactiveCommands<String, String>> reactiveFunction() {
        if (cluster) {
            return c -> ((StatefulRedisClusterConnection<String, String>) c).reactive();
        }
        return c -> ((StatefulRedisConnection<String, String>) c).reactive();
    }

    @Override
    public String toString() {
        if (cluster) {
            return "cluster " + redisUri();
        }
        return redisUri();
    }

}

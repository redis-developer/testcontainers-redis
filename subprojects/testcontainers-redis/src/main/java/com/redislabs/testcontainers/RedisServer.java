package com.redislabs.testcontainers;

import org.testcontainers.containers.GenericContainer;

public interface RedisServer {

    String getRedisURI();

    boolean isCluster();

    static String redisURI(String host, int port) {
        return "redis://" + host + ":" + port;
    }

    static String redisURI(GenericContainer<?> container) {
        return redisURI(container.getHost(), container.getFirstMappedPort());
    }

}

package com.redislabs.testcontainers;

import org.testcontainers.containers.GenericContainer;

public class RedisContainerUtils {

    public static String redisURI(String host, int port) {
        return "redis://" + host + ":" + port;
    }

    public static String redisURI(GenericContainer<?> container) {
        return redisURI(container.getHost(), container.getFirstMappedPort());
    }

}

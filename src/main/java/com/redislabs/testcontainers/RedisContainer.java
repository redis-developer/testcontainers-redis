package com.redislabs.testcontainers;

public interface RedisContainer {

    String getRedisURI();

    boolean isCluster();

}

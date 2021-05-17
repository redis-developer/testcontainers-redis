package com.redislabs.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.org.apache.commons.lang.ClassUtils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

public class RedisEnterpriseContainer extends GenericContainer<RedisEnterpriseContainer> implements RedisContainer {

    public static final String DEFAULT_IMAGE_NAME = "redislabs/redis:latest";
    public static final List<Integer> PORTS = Arrays.asList(53, 5353, 8001, 8070, 8080, 8443, 9443, 12000);

    private static final int DEFAULT_STARTUP_TIMEOUT_SECONDS = 240;
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 120;

    public RedisEnterpriseContainer() {
        super(DEFAULT_IMAGE_NAME);
        withPrivilegedMode(true);
        setStartupAttempts(1);
        withStartupTimeout(Duration.of(2, ChronoUnit.MINUTES));
        withExposedPorts(PORTS.toArray(new Integer[0]));
        PORTS.forEach(p -> addFixedExposedPort(p, p));
    }

    @Override
    public boolean isCluster() {
        return false;
    }

    /**
     * Get Redis URI.
     *
     * @return Redis URI.
     */
    @Override
    public String getRedisURI() {
        return RedisContainerUtils.redisURI(this);
    }

    @Override
    public String toString() {
        return ClassUtils.getShortClassName(getClass());
    }

}

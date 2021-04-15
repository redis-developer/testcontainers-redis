package com.redislabs.testcontainers;

import com.redislabs.mesclun.RedisModulesClient;
import io.lettuce.core.api.StatefulConnection;
import org.testcontainers.utility.DockerImageName;

import java.util.function.Supplier;

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
    public Supplier<StatefulConnection<String, String>> connectionSupplier() {
        if (cluster) {
            throw new UnsupportedOperationException("Redis Modules container does not support cluster mode");
        }
        return RedisModulesClient.create(redisUri())::connect;
    }
}

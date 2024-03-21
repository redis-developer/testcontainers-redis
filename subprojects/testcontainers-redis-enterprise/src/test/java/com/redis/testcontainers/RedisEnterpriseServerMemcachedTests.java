package com.redis.testcontainers;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.redis.enterprise.Database;
import com.redis.enterprise.Database.Type;
import com.redis.enterprise.testcontainers.RedisEnterpriseServer;

@EnabledIfEnvironmentVariable(named = RedisEnterpriseServer.ENV_HOST, matches = ".*")
@TestInstance(Lifecycle.PER_CLASS)
class RedisEnterpriseServerMemcachedTests extends AbstractMemcachedTestBase {

	public static final String DEFAULT_DATABASE_NAME = "testcontainers-memcached";
	public static final int DEFAULT_DATABASE_SHARD_COUNT = 2;
	public static final int DEFAULT_DATABASE_PORT = 12001;
	private static final RedisEnterpriseServer server = new RedisEnterpriseServer().withDatabase(database());

	@Override
	protected RedisEnterpriseServer getMemcachedServer() {
		return server;
	}

	private static Database database() {
		return Database.builder().name(DEFAULT_DATABASE_NAME).type(Type.MEMCACHED)
				.shardCount(DEFAULT_DATABASE_SHARD_COUNT).port(DEFAULT_DATABASE_PORT).build();
	}

}
package com.redis.testcontainers;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.redis.enterprise.Database;
import com.redis.enterprise.testcontainers.RedisEnterpriseContainer;
import com.redis.enterprise.testcontainers.RedisEnterpriseServer;

@EnabledIfEnvironmentVariable(named = RedisEnterpriseServer.ENV_HOST, matches = ".*")
class RedisEnterpriseServerTests extends AbstractRedisTestBase {

	private static final RedisEnterpriseServer server = new RedisEnterpriseServer().withDatabase(database());

	@Override
	protected RedisEnterpriseServer getRedisServer() {
		return server;
	}

	private static Database database() {
		Database database = RedisEnterpriseContainer.defaultDatabase();
		database.setPort(12001);
		return database;
	}

}
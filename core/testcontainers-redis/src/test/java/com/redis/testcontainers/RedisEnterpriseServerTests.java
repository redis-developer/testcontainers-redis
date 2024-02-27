package com.redis.testcontainers;

import org.junit.jupiter.api.Disabled;

import com.redis.enterprise.RedisModule;

@Disabled
class RedisEnterpriseServerTests extends AbstractTestBase {

	private static final RedisEnterpriseServer redis = redisEnterpriseServer();

	@Override
	protected RedisEnterpriseServer getRedisServer() {
		return redis;
	}

	private static RedisEnterpriseServer redisEnterpriseServer() {
		RedisEnterpriseServer server = new RedisEnterpriseServer();
		server.getDatabase().setModules(RedisModule.JSON, RedisModule.SEARCH, RedisModule.TIMESERIES);
		return server;
	}

}

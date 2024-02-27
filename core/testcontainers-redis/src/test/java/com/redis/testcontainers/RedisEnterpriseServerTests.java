package com.redis.testcontainers;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;

import com.redis.enterprise.Database.ModuleConfig;
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
		server.getDatabase().setModules(Stream.of(RedisModule.JSON, RedisModule.SEARCH, RedisModule.TIMESERIES)
				.map(RedisModule::getModuleName).map(ModuleConfig::new).collect(Collectors.toList()));
		return server;
	}

}

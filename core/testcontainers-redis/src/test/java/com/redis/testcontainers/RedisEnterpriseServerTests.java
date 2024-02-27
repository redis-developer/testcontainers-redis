package com.redis.testcontainers;

import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisabledOnOs(value = OS.LINUX)
class RedisEnterpriseServerTests extends AbstractTestBase {

	private static final RedisEnterpriseServer redis = new RedisEnterpriseServer().withHost("nuc");

	@Override
	protected RedisEnterpriseServer getRedisServer() {
		return redis;
	}

}

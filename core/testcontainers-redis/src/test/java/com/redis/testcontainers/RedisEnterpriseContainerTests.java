package com.redis.testcontainers;

import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.redis.enterprise.Database;
import com.redis.enterprise.RedisModule;

@EnabledOnOs(value = OS.LINUX)
class RedisEnterpriseContainerTests extends AbstractTestBase {

	private static final RedisEnterpriseContainer redisEnterprise = new RedisEnterpriseContainer(
			RedisEnterpriseContainer.DEFAULT_IMAGE_NAME.withTag("latest"))
			.withDatabase(Database.builder().name("RedisEnterpriseContainerTests").ossCluster(true)
					.modules(RedisModule.SEARCH, RedisModule.TIMESERIES).build());

	@Override
	protected RedisEnterpriseContainer getRedisServer() {
		return redisEnterprise;
	}

}

package com.redis.testcontainers;

import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.redis.enterprise.Database;
import com.redis.enterprise.RedisModule;

@Testcontainers
@EnabledOnOs(value = OS.LINUX)
class RedisEnterpriseContainerTests extends AbstractTestBase {

	private static final RedisEnterpriseContainer container = new RedisEnterpriseContainer(
			RedisEnterpriseContainer.DEFAULT_IMAGE_NAME.withTag("latest"));

	@Override
	protected RedisEnterpriseContainer getRedisServer() {
		return container;
	}

	public static Database database(String name) {
		return Database.builder().name(name).ossCluster(true).modules(RedisModule.SEARCH, RedisModule.TIMESERIES)
				.build();
	}

}

package com.redis.enterprise;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "REDIS_ENTERPRISE_HOST", matches = ".*")
class RedisEnterpriseServerAdminTests extends AbstractAdminTests {

	@Override
	protected Admin admin() {
		Admin admin = new Admin();
		admin.withHost(System.getenv("REDIS_ENTERPRISE_HOST"));
		return admin;
	}

}

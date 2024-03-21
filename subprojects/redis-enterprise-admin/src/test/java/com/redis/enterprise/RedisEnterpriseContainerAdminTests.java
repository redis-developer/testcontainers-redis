package com.redis.enterprise;

import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@EnabledOnOs(value = OS.LINUX)
class RedisEnterpriseContainerAdminTests extends AbstractAdminTests {

	@Container
	private static RedisEnterpriseContainer container = new RedisEnterpriseContainer(
			RedisEnterpriseContainer.DEFAULT_IMAGE_NAME.withTag(RedisEnterpriseContainer.DEFAULT_TAG));

	@Override
	protected Admin admin() {
		Admin admin = new Admin();
		admin.withHost(container.getHost());
		return admin;
	}

}

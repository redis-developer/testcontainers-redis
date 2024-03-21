package com.redis.testcontainers;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
class MemcachedContainerTests extends AbstractMemcachedTestBase {

	private static final MemcachedContainer server = new MemcachedContainer(
			MemcachedContainer.DEFAULT_IMAGE_NAME.withTag(MemcachedContainer.DEFAULT_TAG));

	@Override
	protected MemcachedContainer getMemcachedServer() {
		return server;
	}

}
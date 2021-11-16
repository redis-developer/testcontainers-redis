package com.redis.testcontainers.junit.jupiter;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

class RedisTestContextsProvider implements ArgumentsProvider {

	@Override
	public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
		AbstractTestcontainersRedisTestBase instance = (AbstractTestcontainersRedisTestBase) context.getRequiredTestInstance();
		return instance.getContexts().stream().map(Arguments::of);
	}

}
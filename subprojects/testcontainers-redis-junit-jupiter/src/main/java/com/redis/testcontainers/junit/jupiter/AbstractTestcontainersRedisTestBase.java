package com.redis.testcontainers.junit.jupiter;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.redis.testcontainers.RedisServer;

/**
 * <code>
 * 
 * &#64;ParameterizedTest
 * &#64;RedisTestContextsSource
 * void myTest(RedisTestContext redis) {
 *    ...
 * }
 * </code>
 * 
 * @author jruaux
 *
 */
@Testcontainers
@TestInstance(Lifecycle.PER_CLASS)
public abstract class AbstractTestcontainersRedisTestBase {

	private Map<RedisServer, RedisTestContext> contexts;

	protected abstract Collection<RedisServer> servers();

	protected Collection<RedisServer> testServers() {
		return servers();
	}

	protected Collection<RedisTestContext> getAllContexts() {
		return contexts.values();
	}

	protected Collection<RedisTestContext> getTestContexts() {
		return testServers().stream().map(contexts::get).collect(Collectors.toList());
	}

	@BeforeAll
	protected void setupContexts() {
		contexts = servers().stream().collect(Collectors.toMap(s -> s, RedisTestContext::new));
	}

	@BeforeEach
	protected void flushAll() {
		contexts.forEach((k, v) -> {
			v.sync().flushall();
			Awaitility.await().until(() -> v.sync().dbsize() == 0);
		});
	}

	@AfterAll
	protected void teardownContexts() {
		contexts.values().forEach(RedisTestContext::close);
		contexts.clear();
	}

}

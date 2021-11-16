package com.redis.testcontainers.junit.jupiter;

import java.util.Collection;
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

	private Collection<RedisTestContext> contexts;

	protected abstract Collection<RedisServer> servers();

	protected Collection<RedisTestContext> getContexts() {
		return contexts;
	}

	@BeforeAll
	protected void setupContexts() {
		contexts = servers().stream().map(RedisTestContext::new).collect(Collectors.toList());
	}

	@BeforeEach
	protected void flushAll() {
		contexts.forEach(c -> {
			c.sync().flushall();
			Awaitility.await().until(() -> c.sync().dbsize() == 0);
		});
	}

	@AfterAll
	protected void teardownContexts() {
		contexts.forEach(RedisTestContext::close);
	}

}

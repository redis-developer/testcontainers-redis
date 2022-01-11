package com.redis.testcontainers.junit;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger log = LoggerFactory.getLogger(AbstractTestcontainersRedisTestBase.class);

	private Map<RedisServer, RedisTestContext> contexts = new LinkedHashMap<>();

	protected abstract Collection<RedisServer> redisServers();

	@BeforeAll
	protected void setup() {
		Assumptions.assumeTrue(redisServers().stream().anyMatch(RedisServer::isActive));
		for (RedisServer server : redisServers()) {
			if (!server.isActive()) {
				continue;
			}
			log.info("Starting container {}", server);
			server.start();
			contexts.put(server, new RedisTestContext(server));
		}
	}

	protected RedisTestContext removeRedisTestContext(RedisServer server) {
		return contexts.remove(server);
	}

	public RedisTestContext getRedisTestContext(RedisServer server) {
		return contexts.get(server);
	}

	protected Collection<RedisTestContext> getRedisTestContexts() {
		return contexts.values();
	}

	@BeforeEach
	protected void flushAll() {
		contexts.forEach((k, v) -> {
			v.sync().flushall();
			Awaitility.await().until(() -> v.sync().dbsize() == 0);
		});
	}

	@AfterAll
	protected void teardown() {
		contexts.values().forEach(RedisTestContext::close);
		contexts.keySet().forEach(RedisServer::close);
		contexts.clear();
	}

}

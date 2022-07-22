package com.redis.testcontainers.junit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
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
public abstract class AbstractTestcontainersRedisTestBase implements RedisTestInstance {

	private static final Logger log = LoggerFactory.getLogger(AbstractTestcontainersRedisTestBase.class);

	private Map<RedisServer, RedisTestContext> contexts = new LinkedHashMap<>();

	protected abstract Collection<RedisServer> redisServers();

	protected Collection<RedisServer> testRedisServers() {
		return contexts.keySet();
	}

	@BeforeAll
	protected void setup() {
		Collection<RedisServer> allRedisServers = redisServers();
		Assumptions.assumeTrue(allRedisServers.stream().anyMatch(RedisServer::isEnabled));
		for (RedisServer server : allRedisServers) {
			if (server.isEnabled()) {
				log.info("Starting container {}", server);
				server.start();
				contexts.put(server, new RedisTestContext(server));
			} else {
				log.info("Container {} disabled", server);
			}
		}
	}

	protected RedisTestContext removeRedisTestContext(RedisServer server) {
		return contexts.remove(server);
	}

	private List<RedisTestContext> contexts(Collection<RedisServer> servers) {
		List<RedisTestContext> testContexts = new ArrayList<>();
		for (RedisServer server : servers) {
			if (contexts.containsKey(server)) {
				testContexts.add(contexts.get(server));
			}
		}
		return testContexts;
	}

	@BeforeEach
	protected void flushAll() {
		contexts.forEach((k, v) -> {
			if (k.isEnabled()) {
				Awaitility.await().until(() -> {
					v.sync().flushall();
					return v.sync().dbsize() == 0;
				});
			}
		});
	}

	@AfterAll
	protected void teardown() {
		contexts.forEach((k, v) -> {
			v.close();
			k.close();
		});
		contexts.clear();
	}

	public RedisTestContext getContext(RedisServer server) {
		return contexts.get(server);
	}

	public List<RedisTestContext> getAllContexts() {
		return new ArrayList<>(contexts.values());
	}

	@Override
	public List<RedisTestContext> getContexts() {
		return contexts(testRedisServers());
	}

}

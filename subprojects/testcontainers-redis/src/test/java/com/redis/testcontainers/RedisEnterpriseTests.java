package com.redis.testcontainers;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.redis.lettucemod.RedisModulesClient;
import com.redis.lettucemod.Utils;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.lettucemod.api.search.Field;
import com.redis.lettucemod.api.search.SearchResults;
import com.redis.lettucemod.cluster.RedisModulesClusterClient;
import com.redis.lettucemod.cluster.api.StatefulRedisModulesClusterConnection;
import com.redis.lettucemod.output.ExecutionResults;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;

class RedisEnterpriseTests {

	@Test
	void singleShard() {
		try (RedisEnterpriseContainer container = new RedisEnterpriseContainer()) {
			container.start();
			RedisClient client = RedisClient.create(container.getRedisURI());
			try (StatefulRedisConnection<String, String> connection = client.connect()) {
				Assertions.assertEquals("PONG", connection.sync().ping());
			} finally {
				client.shutdown();
				client.getResources().shutdown();
			}
			container.stop();
		}
	}

	@Test
	void cluster() {
		try (RedisEnterpriseContainer container = new RedisEnterpriseContainer()) {
			container.withShardCount(3);
			container.withOSSCluster();
			container.start();
			RedisClusterClient client = RedisClusterClient.create(container.getRedisURI());
			try (StatefulRedisClusterConnection<String, String> connection = client.connect()) {
				Assertions.assertEquals("PONG", connection.sync().ping());
			} finally {
				client.shutdown();
				client.getResources().shutdown();
			}
			container.stop();
		}
	}

	@Test
	void searchCluster() {
		try (RedisEnterpriseContainer container = new RedisEnterpriseContainer()) {
			container.withShardCount(3);
			container.withOSSCluster();
			container.withModules(com.redis.testcontainers.RedisEnterpriseContainer.RedisModule.SEARCH);
			container.start();
			RedisModulesClusterClient client = RedisModulesClusterClient.create(container.getRedisURI());
			try (StatefulRedisModulesClusterConnection<String, String> connection = client.connect()) {
				connection.sync().create("test", Field.text("name").build(), Field.tag("id").build());
				for (int index = 0; index < 10; index++) {
					Map<String, String> hash = new HashMap<>();
					hash.put("name", "name " + index);
					hash.put("id", String.valueOf(index + 1));
					connection.sync().hset("hash:" + index, hash);
				}
				SearchResults<String, String> results = connection.sync().search("test", "*");
				Assertions.assertEquals(10, results.getCount());
			} finally {
				client.shutdown();
				client.getResources().shutdown();
			}
			container.stop();
		}
	}

	@Test
	void gears() {
		try (RedisEnterpriseContainer container = new RedisEnterpriseContainer()) {
			container.withModules(com.redis.testcontainers.RedisEnterpriseContainer.RedisModule.GEARS);
			container.start();
			RedisModulesClient client = RedisModulesClient.create(container.getRedisURI());
			try (StatefulRedisModulesConnection<String, String> connection = client.connect()) {
				connection.sync().set("foo", "bar");
				ExecutionResults results = connection.sync().pyexecute(load("sleep.py"));
				Assertions.assertEquals(0, results.getErrors().size());
			}
			container.stop();
		}
	}

	private String load(String resourceName) {
		return Utils.toString(getClass().getClassLoader().getResourceAsStream(resourceName));
	}

}

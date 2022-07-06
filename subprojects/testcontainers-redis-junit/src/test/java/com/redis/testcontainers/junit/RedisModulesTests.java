package com.redis.testcontainers.junit;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;

import com.redis.enterprise.Database;
import com.redis.enterprise.RedisModule;
import com.redis.lettucemod.api.sync.RedisTimeSeriesCommands;
import com.redis.lettucemod.search.Field;
import com.redis.lettucemod.search.SearchResults;
import com.redis.lettucemod.timeseries.CreateOptions;
import com.redis.lettucemod.timeseries.Label;
import com.redis.testcontainers.RedisContainer;
import com.redis.testcontainers.RedisEnterpriseContainer;
import com.redis.testcontainers.RedisModulesContainer;
import com.redis.testcontainers.RedisServer;
import com.redis.testcontainers.RedisStackContainer;

class RedisModulesTests extends AbstractTestcontainersRedisTestBase {

	private static final RedisContainer REDIS = new RedisContainer(
			RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG));

	private static final RedisModulesContainer REDISMOD = new RedisModulesContainer(
			RedisModulesContainer.DEFAULT_IMAGE_NAME.withTag(RedisModulesContainer.DEFAULT_TAG));

	private static final RedisStackContainer REDIS_STACK = new RedisStackContainer(
			RedisStackContainer.DEFAULT_IMAGE_NAME.withTag(RedisStackContainer.DEFAULT_TAG));

	private static final RedisEnterpriseContainer REDIS_ENTERPRISE = new RedisEnterpriseContainer(
			RedisEnterpriseContainer.DEFAULT_IMAGE_NAME.withTag(RedisEnterpriseContainer.DEFAULT_TAG))
			.withDatabase(Database.name("RedisEnterpriseContainerTests").ossCluster(true)
					.modules(RedisModule.SEARCH, RedisModule.GEARS, RedisModule.TIMESERIES).build());

	@Override
	protected Collection<RedisServer> redisServers() {
		return Arrays.asList(REDIS, REDISMOD, REDIS_STACK, REDIS_ENTERPRISE);
	}

	@Override
	protected Collection<RedisServer> testRedisServers() {
		return Arrays.asList(REDISMOD, REDIS_ENTERPRISE);
	}

//	@ParameterizedTest
//	@RedisTestContextsSource
//	void gearsPyExecute(RedisTestContext context) {
//		RedisModulesCommands<String, String> sync = context.sync();
//		sync.set("foo", "bar");
//		String sleepPy = "def sleep(x):\n" + "    from time import sleep\n" + "    sleep(1)\n" + "    return 1\n" + "\n"
//				+ "GB().map(sleep).run()";
//		Assertions.assertTrue(sync.pyexecute(sleepPy).getErrors().isEmpty());
//		Assertions.assertTrue(sync.pyexecute("GB().run()").isOk());
//	}

	@SuppressWarnings("unchecked")
	@ParameterizedTest
	@RedisTestContextsSource
	void timeSeries(RedisTestContext context) {
		RedisTimeSeriesCommands<String, String> ts = context.sync();
		ts.create("temperature:3:11", CreateOptions.<String, String>builder().retentionPeriod(6000)
				.labels(Label.of("sensor_id", "2"), Label.of("area_id", "32")).build());
		// TS.ADD temperature:3:11 1548149181 30
		Long add1 = ts.add("temperature:3:11", 1548149181, 30);
		Assertions.assertEquals(1548149181, add1);
	}

	@ParameterizedTest
	@RedisTestContextsSource
	void search(RedisTestContext context) {
		context.sync().create("test", Field.text("name").build(), Field.tag("id").build());
		for (int index = 0; index < 10; index++) {
			Map<String, String> hash = new HashMap<>();
			hash.put("name", "name " + index);
			hash.put("id", String.valueOf(index + 1));
			context.sync().hset("hash:" + index, hash);
		}
		SearchResults<String, String> results = context.sync().search("test", "*");
		Assertions.assertEquals(10, results.getCount());
	}

}

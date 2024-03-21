package com.redis.enterprise;

import java.util.List;
import java.util.stream.Stream;

import org.apache.hc.client5.http.HttpResponseException;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;

@TestInstance(Lifecycle.PER_CLASS)
abstract class AbstractAdminTests {

	protected Admin admin;

	@BeforeAll
	void setupAdmin() throws Exception {
		admin = admin();
	}

	protected abstract Admin admin();

	@AfterAll
	void teardownAdmin() throws Exception {
		admin.close();
	}

	@BeforeEach
	void deleteAllDatabases() throws Exception {
		List<Database> databases = admin.getDatabases();
		for (Database database : databases) {
			admin.deleteDatabase(database.getUid());
		}
		Awaitility.await().until(() -> admin.getDatabases().isEmpty());
	}

	@Test
	void createDatabase() throws Exception {
		String databaseName = "CreateDBTest";
		admin.createDatabase(Database.builder().name(databaseName).build());
		Stream<Database> stream = admin.getDatabases().stream().filter(d -> d.getName().equals(databaseName));
		Assertions.assertEquals(1, stream.count());
	}

	@Test
	void createClusterDatabase() throws Exception {
		String databaseName = "CreateClusterDBTest";
		admin.createDatabase(Database.builder().name(databaseName).ossCluster(true).port(12000).build());
		List<Database> databases = admin.getDatabases();
		Assertions.assertEquals(1, databases.size());
		Assertions.assertEquals(databaseName, databases.get(0).getName());
		Database database = databases.get(0);
		RedisClusterClient client = RedisClusterClient.create(RedisURI.create(admin.getHost(), database.getPort()));
		try (StatefulRedisClusterConnection<String, String> connection = client.connect()) {
			Assertions.assertEquals("PONG", connection.sync().ping());
		}
		client.shutdown();
		client.getResources().shutdown();
	}

	@Test
	void createSearchDatabase() throws Exception {
		String databaseName = "CreateSearchDBTest";
		admin.createDatabase(Database.builder().name(databaseName).module(RedisModule.SEARCH).build());
		List<Database> databases = admin.getDatabases();
		Assertions.assertEquals(1, databases.size());
		Assertions.assertEquals(RedisModule.SEARCH.getModuleName(), databases.get(0).getModules().get(0).getName());
	}

	@Test
	void deleteDatabase() throws Exception {
		String databaseName = "DeleteDBTest";
		Database database = admin.createDatabase(Database.builder().name(databaseName).build());
		admin.deleteDatabase(database.getUid());
		Awaitility.await().until(() -> admin.getDatabases().stream().noneMatch(d -> d.getUid() == database.getUid()));
	}

	@Test
	void createDatabaseException() throws Exception {
		long memory = 999 * Database.GIGA;
		Assertions.assertThrows(HttpResponseException.class, () -> admin
				.createDatabase(Database.builder().name("DatabaseCreateExceptionTestDB").memory(memory).build()));
	}

}

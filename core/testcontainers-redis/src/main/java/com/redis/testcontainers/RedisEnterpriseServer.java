package com.redis.testcontainers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.lifecycle.Startable;

import com.redis.enterprise.Admin;
import com.redis.enterprise.Database;

public class RedisEnterpriseServer implements RedisServer, Startable {

	public static final String DEFAULT_HOST = "localhost";
	public static final String DEFAULT_ADMIN_USERNAME = "admin@redis.com";
	public static final String DEFAULT_ADMIN_PASSWORD = "redis123";
	public static final int DEFAULT_ADMIN_PORT = 8443;
	public static final int DEFAULT_DATABASE_PORT = 12000;
	public static final int DEFAULT_DATABASE_SHARD_COUNT = 2;
	public static final String DEFAULT_DATABASE_NAME = "testcontainers";

	private static final Logger log = LoggerFactory.getLogger(RedisEnterpriseServer.class);

	private String host = DEFAULT_HOST;
	private int adminPort = DEFAULT_ADMIN_PORT;
	private String adminUsername = DEFAULT_ADMIN_USERNAME;
	private String adminPassword = DEFAULT_ADMIN_PASSWORD;

	private Database database = defaultDatabase().build();

	private Database runningDatabase;

	public Database getDatabase() {
		return database;
	}

	public static Database.Builder defaultDatabase() {
		return Database.builder().name(DEFAULT_DATABASE_NAME).shardCount(DEFAULT_DATABASE_SHARD_COUNT)
				.port(DEFAULT_DATABASE_PORT);
	}

	public RedisEnterpriseServer withDatabase(Database database) {
		this.database = database;
		return this;
	}

	public int getAdminPort() {
		return adminPort;
	}

	public RedisEnterpriseServer withAdminPort(int port) {
		this.adminPort = port;
		return this;
	}

	public String getAdminPassword() {
		return adminPassword;
	}

	public RedisEnterpriseServer withAdminPassword(String password) {
		this.adminPassword = password;
		return this;
	}

	public String getAdminUsername() {
		return adminUsername;
	}

	public RedisEnterpriseServer withAdminUsername(String username) {
		this.adminUsername = username;
		return this;
	}

	public String getHost() {
		return host;
	}

	public RedisEnterpriseServer withHost(String host) {
		this.host = host;
		return this;
	}

	@Override
	public String getRedisHost() {
		return getHost();
	}

	@Override
	public int getRedisPort() {
		return database.getPort();
	}

	@Override
	public boolean isRedisCluster() {
		return database.isOssCluster();
	}

	@Override
	public synchronized void start() {
		if (runningDatabase == null) {
			try (Admin admin = admin()) {
				runningDatabase = admin.createDatabase(database);
				log.info("Created database {} with UID {}", runningDatabase.getName(), runningDatabase.getUid());
			} catch (Exception e) {
				throw new ContainerLaunchException("Could not initialize Redis Enterprise database", e);
			}
		}
	}

	private Admin admin() {
		Admin admin = new Admin();
		admin.withUserName(adminUsername);
		admin.withPassword(adminPassword);
		admin.withHost(host);
		return admin;
	}

	@Override
	public synchronized void stop() {
		if (runningDatabase != null) {
			try (Admin admin = admin()) {
				admin.deleteDatabase(runningDatabase.getUid());
			} catch (Exception e) {
				throw new ContainerLaunchException("Could not tear down Redis Enterprise database", e);
			}
		}
	}

}

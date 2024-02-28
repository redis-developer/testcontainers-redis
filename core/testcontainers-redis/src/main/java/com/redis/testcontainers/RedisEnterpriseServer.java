package com.redis.testcontainers;

import java.io.IOException;
import java.util.Optional;

import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.lifecycle.Startable;

import com.redis.enterprise.Admin;
import com.redis.enterprise.Database;

public class RedisEnterpriseServer implements RedisServer, Startable {

	private static final String ENV_PREFIX = "REDIS_ENTERPRISE_";
	public static final String ENV_HOST = ENV_PREFIX + "HOST";
	public static final String ENV_USER = ENV_PREFIX + "USER";
	public static final String ENV_PASSWORD = ENV_PREFIX + "PASSWORD";
	public static final String ENV_PORT = ENV_PREFIX + "PORT";
	public static final String DEFAULT_HOST = "localhost";
	public static final String DEFAULT_USER = "admin@redis.com";
	public static final String DEFAULT_PASSWORD = "redis123";
	public static final int DEFAULT_PORT = 8443;

	private String host = getenv(ENV_HOST, DEFAULT_HOST);
	private int adminPort = getenvInt(ENV_PORT, DEFAULT_PORT);
	private String adminUsername = getenv(ENV_USER, DEFAULT_USER);
	private String adminPassword = getenv(ENV_PASSWORD, DEFAULT_PASSWORD);
	private Database database = RedisEnterpriseContainer.defaultDatabase();

	private Database runningDatabase;
	private Admin admin;

	private static String getenv(String name, String defaultValue) {
		return getenv(name).orElse(defaultValue);
	}

	private static Optional<String> getenv(String name) {
		String value = System.getenv(name);
		if (value == null || value.length() == 0) {
			return Optional.empty();
		}
		return Optional.of(value);
	}

	private static int getenvInt(String name, int defaultValue) {
		return getenv(name).map(Integer::parseInt).orElse(defaultValue);
	}

	public Database getDatabase() {
		return database;
	}

	public Admin getAdmin() {
		return admin;
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
		return runningDatabase.getPort();
	}

	@Override
	public boolean isRedisCluster() {
		return runningDatabase.isOssCluster();
	}

	@Override
	public synchronized void start() {
		if (admin == null) {
			admin = new Admin();
			admin.withUserName(adminUsername);
			admin.withPassword(adminPassword);
			admin.withHost(host);
		}
		if (runningDatabase == null) {
			try {
				admin.getDatabases().stream().filter(d -> d.getName().equals(database.getName())).map(Database::getUid)
						.forEach(admin::deleteDatabase);
				runningDatabase = admin.createDatabase(database);
			} catch (Exception e) {
				throw new ContainerLaunchException("Could not initialize Redis Enterprise database", e);
			}
		}
	}

	@Override
	public synchronized void stop() {
		if (runningDatabase != null) {
			admin.deleteDatabase(runningDatabase.getUid());
			runningDatabase = null;
		}
		if (admin != null) {
			try {
				admin.close();
				admin = null;
			} catch (IOException e) {
				throw new ContainerLaunchException("Could not close Redis Enterprise admin", e);
			}
		}
	}

}

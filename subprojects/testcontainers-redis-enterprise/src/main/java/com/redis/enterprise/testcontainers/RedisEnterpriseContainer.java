package com.redis.enterprise.testcontainers;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.utility.DockerImageName;

import com.redis.enterprise.Admin;
import com.redis.enterprise.Database;
import com.redis.enterprise.RedisModule;

public class RedisEnterpriseContainer extends AbstractRedisEnterpriseContainer<RedisEnterpriseContainer> {

	public static final int DEFAULT_DATABASE_SHARD_COUNT = 2;
	public static final int DEFAULT_DATABASE_PORT = 12000;
	public static final String DEFAULT_DATABASE_NAME = "testcontainers";
	protected static final RedisModule[] DEFAULT_DATABASE_MODULES = { RedisModule.JSON, RedisModule.SEARCH,
			RedisModule.TIMESERIES, RedisModule.BLOOM };

	private static final Logger log = LoggerFactory.getLogger(RedisEnterpriseContainer.class);

	private final Admin admin = new Admin();
	private Database database = defaultDatabase();

	public static Database defaultDatabase() {
		return Database.builder().name(DEFAULT_DATABASE_NAME).shardCount(DEFAULT_DATABASE_SHARD_COUNT)
				.port(DEFAULT_DATABASE_PORT).ossCluster(true).modules(DEFAULT_DATABASE_MODULES).build();
	}

	public RedisEnterpriseContainer(String dockerImageName) {
		super(dockerImageName);
	}

	public RedisEnterpriseContainer(final DockerImageName dockerImageName) {
		super(dockerImageName);
	}

	public Database getDatabase() {
		return database;
	}

	public RedisEnterpriseContainer withDatabase(Database database) {
		this.database = database;
		if (database.getPort() == null) {
			database.setPort(DEFAULT_DATABASE_PORT);
		}
		return this;
	}

	@Override
	protected String getAdminUserName() {
		return admin.getUserName();
	}

	@Override
	protected String getAdminPassword() {
		return admin.getPassword();
	}

	@Override
	protected void doStart() {
		admin.withHost(getHost());
		addFixedExposedPort(admin.getPort(), admin.getPort());
		addFixedExposedPort(database.getPort(), database.getPort());
		super.doStart();
	}

	@Override
	protected void createCluster() {
		log.info("Waiting for cluster bootstrap");
		admin.waitForBoostrap();
		super.createCluster();
		Database response;
		try {
			response = admin.createDatabase(database);
		} catch (Exception e) {
			throw new ContainerLaunchException("Could not create database", e);
		}
		log.info("Created database {} with UID {}", response.getName(), response.getUid());
	}

	@Override
	public boolean isRedisCluster() {
		return database.isOssCluster();
	}

	@Override
	public int getRedisPort() {
		return database.getPort();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(database);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		RedisEnterpriseContainer other = (RedisEnterpriseContainer) obj;
		return Objects.equals(database, other.database);
	}

}

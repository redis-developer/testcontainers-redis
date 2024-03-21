package com.redis.enterprise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.utility.DockerImageName;

import com.redis.enterprise.testcontainers.AbstractRedisEnterpriseContainer;

public class RedisEnterpriseContainer extends AbstractRedisEnterpriseContainer<RedisEnterpriseContainer> {

	private final Admin admin = new Admin();
	private Database database = Database.builder().shardCount(2).port(12000).ossCluster(true)
			.modules(RedisModule.SEARCH, RedisModule.JSON, RedisModule.TIMESERIES, RedisModule.BLOOM).build();

	private final Logger log = LoggerFactory.getLogger(RedisEnterpriseContainer.class);

	public RedisEnterpriseContainer(String dockerImageName) {
		super(dockerImageName);
	}

	public RedisEnterpriseContainer(DockerImageName dockerImageName) {
		super(dockerImageName);
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
	public int getRedisPort() {
		return database.getPort();
	}

	@Override
	public boolean isRedisCluster() {
		return database.isOssCluster();
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

}

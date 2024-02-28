package com.redis.testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.org.apache.commons.lang3.ClassUtils;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestEnvironment;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectExecResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.redis.enterprise.Admin;
import com.redis.enterprise.Database;
import com.redis.enterprise.RedisModule;

public class RedisEnterpriseContainer extends GenericContainer<RedisEnterpriseContainer> implements RedisServer {

	public static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("redislabs/redis");
	public static final String DEFAULT_TAG = "latest";
	public static final int DEFAULT_DATABASE_SHARD_COUNT = 2;
	public static final int DEFAULT_DATABASE_PORT = 12000;
	public static final String DEFAULT_DATABASE_NAME = "testcontainers";
	public static final RedisModule[] DEFAULT_DATABASE_MODULES = { RedisModule.JSON, RedisModule.SEARCH,
			RedisModule.TIMESERIES, RedisModule.BLOOM };

	private static final Logger log = LoggerFactory.getLogger(RedisEnterpriseContainer.class);
	private static final String NODE_EXTERNAL_ADDR = "0.0.0.0";
	private static final String RLADMIN = "/opt/redislabs/bin/rladmin";
	private static final Long EXIT_CODE_SUCCESS = 0L;
	private static final int WEB_UI_PORT = 8443;

	private Database database = defaultDatabase();

	public static Database defaultDatabase() {
		return Database.builder().name(DEFAULT_DATABASE_NAME).shardCount(DEFAULT_DATABASE_SHARD_COUNT)
				.port(DEFAULT_DATABASE_PORT).ossCluster(true).modules(DEFAULT_DATABASE_MODULES).build();
	}

	public RedisEnterpriseContainer(String dockerImageName) {
		this(DockerImageName.parse(dockerImageName));
	}

	public RedisEnterpriseContainer(final DockerImageName dockerImageName) {
		super(dockerImageName);
		addFixedExposedPort(Admin.DEFAULT_PORT, Admin.DEFAULT_PORT);
		addFixedExposedPort(WEB_UI_PORT, WEB_UI_PORT);
		addFixedExposedPort(database.getPort(), database.getPort());
		withPrivilegedMode(true);
		waitingFor(Wait.forLogMessage(".*success: job_scheduler entered RUNNING state, process has stayed up for.*\\n",
				1));
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

	private Admin admin() {
		Admin admin = new Admin();
		admin.withHost(getRedisHost());
		return admin;
	}

	@Override
	protected void containerIsStarted(InspectContainerResponse containerInfo) {
		super.containerIsStarted(containerInfo);
		try (Admin admin = admin()) {
			log.info("Waiting for cluster bootstrap");
			admin.waitForBoostrap();
			createCluster();
			Database response = admin.createDatabase(database);
			log.info("Created database {} with UID {}", response.getName(), response.getUid());
		} catch (Exception e) {
			throw new ContainerLaunchException("Could not initialize Redis Enterprise container", e);
		}
	}

	private void createCluster() {
		log.info("Creating cluster");
		if (!TestEnvironment.dockerExecutionDriverSupportsExec()) {
			// at time of writing, this is the expected result in CircleCI.
			throw new UnsupportedOperationException(
					"Your docker daemon is running the \"lxc\" driver, which doesn't support \"docker exec\".");
		}

		InspectContainerResponse containerInfo = getContainerInfo();
		if (!isRunning(containerInfo)) {
			throw new IllegalStateException("execInContainer can only be used while the Container is running");
		}

		String containerId = containerInfo.getId();
		String containerName = containerInfo.getName();

		DockerClient dockerClient = DockerClientFactory.instance().client();

		String[] commands = new String[] { RLADMIN, "cluster", "create", "name", "cluster.local", "username",
				Admin.DEFAULT_USER_NAME, "password", Admin.DEFAULT_PASSWORD, "external_addr", NODE_EXTERNAL_ADDR };
		log.debug("{}: Running \"exec\" command: {}", containerName, commands);
		final ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
				.withAttachStdout(true).withAttachStderr(true).withCmd(commands).withPrivileged(true).exec();

		final ToStringConsumer stdoutConsumer = new ToStringConsumer();
		final ToStringConsumer stderrConsumer = new ToStringConsumer();

		try (FrameConsumerResultCallback callback = new FrameConsumerResultCallback()) {
			callback.addConsumer(OutputFrame.OutputType.STDOUT, stdoutConsumer);
			callback.addConsumer(OutputFrame.OutputType.STDERR, stderrConsumer);
			dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(callback).awaitCompletion();
			InspectExecResponse execResponse = dockerClient.inspectExecCmd(execCreateCmdResponse.getId()).exec();
			if (EXIT_CODE_SUCCESS.equals(execResponse.getExitCodeLong())) {
				return;
			}
			if (log.isErrorEnabled()) {
				log.error("Could not create cluster: {}", stderrConsumer.toString(StandardCharsets.UTF_8));
			}
			throw new ContainerLaunchException("Could not create cluster");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ContainerLaunchException("Could not create cluster", e);
		} catch (IOException e) {
			log.error("Could not close result callback", e);
		}
	}

	@Override
	public boolean isRedisCluster() {
		return database.isOssCluster();
	}

	@Override
	public String toString() {
		return ClassUtils.getShortClassName(RedisEnterpriseContainer.class);
	}

	private boolean isRunning(InspectContainerResponse containerInfo) {
		try {
			return containerInfo != null && Boolean.TRUE.equals(containerInfo.getState().getRunning());
		} catch (DockerException e) {
			return false;
		}
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

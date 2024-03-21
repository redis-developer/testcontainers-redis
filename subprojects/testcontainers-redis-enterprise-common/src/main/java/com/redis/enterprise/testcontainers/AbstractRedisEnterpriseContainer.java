package com.redis.enterprise.testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
import com.redis.testcontainers.MemcachedServer;
import com.redis.testcontainers.RedisServer;

public abstract class AbstractRedisEnterpriseContainer<T extends AbstractRedisEnterpriseContainer<T>>
		extends GenericContainer<T> implements RedisServer, MemcachedServer {

	public static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("redislabs/redis");
	public static final String DEFAULT_TAG = "latest";

	private static final Logger log = LoggerFactory.getLogger(AbstractRedisEnterpriseContainer.class);
	private static final int WEB_UI_PORT = 8443;
	private static final String NODE_EXTERNAL_ADDR = "0.0.0.0";
	private static final String RLADMIN = "/opt/redislabs/bin/rladmin";
	private static final Long EXIT_CODE_SUCCESS = 0L;

	protected AbstractRedisEnterpriseContainer(String dockerImageName) {
		this(DockerImageName.parse(dockerImageName));
	}

	protected AbstractRedisEnterpriseContainer(final DockerImageName dockerImageName) {
		super(dockerImageName);
		addFixedExposedPort(WEB_UI_PORT, WEB_UI_PORT);
		withPrivilegedMode(true);
		waitingFor(Wait.forLogMessage(".*success: job_scheduler entered RUNNING state, process has stayed up for.*\\n",
				1));
	}

	@Override
	protected void containerIsStarted(InspectContainerResponse containerInfo) {
		super.containerIsStarted(containerInfo);
		try {
			createCluster();
		} catch (Exception e) {
			throw new ContainerLaunchException("Could not initialize Redis Enterprise", e);
		}
	}

	protected void createCluster() throws Exception {
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
				getAdminUserName(), "password", getAdminPassword(), "external_addr", NODE_EXTERNAL_ADDR };
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
	public String toString() {
		return ClassUtils.getShortClassName(getClass());
	}

	protected abstract String getAdminPassword();

	protected abstract String getAdminUserName();

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
	public String getMemcachedHost() {
		return getRedisHost();
	}

	@Override
	public int getMemcachedPort() {
		return getRedisPort();
	}

}

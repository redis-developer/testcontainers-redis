package com.redis.testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestEnvironment;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import com.redis.testcontainers.support.RetryCallable;
import com.redis.testcontainers.support.enterprise.Provisioner;
import com.redis.testcontainers.support.enterprise.RestAPI;
import com.redis.testcontainers.support.enterprise.rest.Bootstrap;
import com.redis.testcontainers.support.enterprise.rest.DatabaseCreateRequest;
import com.redis.testcontainers.support.enterprise.rest.DatabaseCreateResponse;

public class RedisEnterpriseContainer extends GenericContainer<RedisEnterpriseContainer> implements RedisServer {

	public enum RedisModule {

		BLOOM("bf"), GEARS("rg"), GRAPH("graph"), JSON("ReJSON"), SEARCH("search"), TIMESERIES("timeseries");

		private final String name;

		RedisModule(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

	}

	private static final Logger log = LoggerFactory.getLogger(RedisEnterpriseContainer.class);

	public static final String ADMIN_USERNAME = "testcontainers@redis.com";
	public static final String ADMIN_PASSWORD = "redis123";
	public static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("redislabs/redis");
	public static final String DEFAULT_TAG = "6.2.8-50";
	private static final String NODE_EXTERNAL_ADDR = "0.0.0.0";
	private static final int DEFAULT_SHARD_COUNT = 2;
	private static final String DEFAULT_DATABASE_NAME = "testcontainers";
	private static final String RLADMIN = "/opt/redislabs/bin/rladmin";
	private static final Duration DEFAULT_BOOTSTRAP_TIMEOUT = Duration.ofSeconds(30);
	private static final Duration DEFAULT_BOOTSTRAP_RETRY_DELAY = Duration.ofSeconds(3);
	private static final Duration DEFAULT_CLUSTER_CREATE_TIMEOUT = Duration.ofSeconds(10);
	private static final Duration DEFAULT_CLUSTER_CREATE_RETRY_DELAY = Duration.ofSeconds(3);
	public static final int ADMIN_PORT = 8443;
	public static final int ENDPOINT_PORT = 12000;

	private Provisioner provisioner = new Provisioner();
	private String databaseName = DEFAULT_DATABASE_NAME;
	private int shardCount = DEFAULT_SHARD_COUNT;
	private boolean ossCluster;
	private Duration bootstrapTimeout = DEFAULT_BOOTSTRAP_TIMEOUT;
	private Duration bootstrapRetryDelay = DEFAULT_BOOTSTRAP_RETRY_DELAY;
	private Duration clusterCreateTimeout = DEFAULT_CLUSTER_CREATE_TIMEOUT;
	private Duration clusterCreateRetryDelay = DEFAULT_CLUSTER_CREATE_RETRY_DELAY;
	private Set<RedisModule> modules = new HashSet<>();

	/**
	 * @deprecated use {@link RedisEnterpriseContainer(DockerImageName)} instead
	 */
	@Deprecated
	public RedisEnterpriseContainer() {
		this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
	}

	/**
	 * @deprecated use {@link RedisEnterpriseContainer(DockerImageName)} instead
	 */
	@Deprecated
	public RedisEnterpriseContainer(final String tag) {
		this(DEFAULT_IMAGE_NAME.withTag(tag));
	}

	public RedisEnterpriseContainer(final DockerImageName dockerImageName) {
		super(dockerImageName);
		addFixedExposedPort(RestAPI.DEFAULT_PORT, RestAPI.DEFAULT_PORT);
		addFixedExposedPort(ADMIN_PORT, ADMIN_PORT);
		addFixedExposedPort(ENDPOINT_PORT, ENDPOINT_PORT);
		withPrivilegedMode(true);
		withPublishAllPorts(false);
		waitingFor(Wait.forLogMessage(".*success: job_scheduler entered RUNNING state, process has stayed up for.*\\n",
				1));

	}

	public RedisEnterpriseContainer withBootstrapTimeout(Duration timeout) {
		this.bootstrapTimeout = timeout;
		return this;
	}

	public RedisEnterpriseContainer withBootstrapRetryDelay(Duration retryDelay) {
		this.bootstrapRetryDelay = retryDelay;
		return this;
	}

	public RedisEnterpriseContainer withClusterCreateTimeout(Duration timeout) {
		this.clusterCreateTimeout = timeout;
		return this;
	}

	public RedisEnterpriseContainer withClusterCreateRetryDelay(Duration retryDelay) {
		this.clusterCreateRetryDelay = retryDelay;
		return this;
	}

	public RedisEnterpriseContainer withProvisioner(Provisioner provisioner) {
		this.provisioner = provisioner;
		return this;
	}

	public RedisEnterpriseContainer withShardCount(int shardCount) {
		this.shardCount = shardCount;
		return this;
	}

	public RedisEnterpriseContainer withModule(RedisModule module) {
		this.modules.add(module);
		return this;
	}

	public RedisEnterpriseContainer withModules(RedisModule... modules) {
		this.modules.addAll(Arrays.asList(modules));
		return this;
	}

	public RedisEnterpriseContainer withDatabaseName(String name) {
		this.databaseName = name;
		return this;
	}

	public RedisEnterpriseContainer withOSSCluster() {
		this.ossCluster = true;
		return this;
	}

	@Override
	protected void containerIsStarted(InspectContainerResponse containerInfo) {
		super.containerIsStarted(containerInfo);
		RestAPI restAPI = new RestAPI();
		restAPI.withHost(getHost());
		try {
			waitForBootstrap(restAPI);
		} catch (Exception e) {
			throw new RuntimeException("Error while waiting for bootstrap", e);
		}
		String username = ADMIN_USERNAME;
		String password = ADMIN_PASSWORD;
		String externalAddress = NODE_EXTERNAL_ADDR;
		log.info("Creating cluster with username={}, password={}, external_adr={}", username, password,
				externalAddress);
		createCluster(username, password, externalAddress);
		String host = getHost();
		log.info("Creating REST API client with username={}, password={}, host={}", username, password, host);
		restAPI.withCredentials(new UsernamePasswordCredentials(username, password.toCharArray()));
		provisioner.withRestAPI(restAPI);
		DatabaseCreateRequest database = new DatabaseCreateRequest();
		database.setName(databaseName);
		database.setPort(ENDPOINT_PORT);
		database.setShardCount(shardCount);
		database.setOssCluster(ossCluster);
		database.setModules(modules.stream().map(RedisModule::getName).collect(Collectors.toList()));
		DatabaseCreateResponse response;
		try {
			response = provisioner.create(database);
		} catch (Exception e) {
			throw new RuntimeException("Could not provision database", e);
		}
		log.info("Created database {} with UID {}", database.getName(), response.getUid());
	}

	private void waitForBootstrap(RestAPI restAPI) throws Exception {
		RetryCallable<Bootstrap> callable = RetryCallable.delegate(() -> {
			log.info("Checking bootstrap status");
			Bootstrap bootstrap = restAPI.bootstrap();
			if ("idle".equals(bootstrap.getStatus().getState())) {
				return bootstrap;
			}
			throw new ContainerLaunchException("Timed out waiting for bootstrap");
		}).sleep(bootstrapRetryDelay).timeout(bootstrapTimeout);
		callable.call();
	}

	private void createCluster(String username, String password, String externalAddress) {
		try {
			RetryCallable.delegate(() -> {
				ExecResult result = execute(RLADMIN, "cluster", "create", "name", "cluster.local", "username", username,
						"password", password, "external_addr", externalAddress);
				if (result.getExitCode() == 0) {
					return result;
				}
				throw new Exception("Failed to create cluster: " + result.getStderr() + " " + result.getStdout());
			}).sleep(clusterCreateRetryDelay).timeout(clusterCreateTimeout).call();
		} catch (Exception e) {
			throw new ContainerLaunchException("Could not create Redis Enterprise cluster", e);
		}
	}

	@Override
	public String getRedisURI() {
		return RedisServer.redisURI(getHost(), ENDPOINT_PORT);
	}

	@Override
	public boolean isCluster() {
		return ossCluster;
	}

	@Override
	public String toString() {
		return "RedisEnterpriseContainer " + getRedisURI();
	}

	@SuppressWarnings("deprecation")
	public ExecResult execute(String... command)
			throws UnsupportedOperationException, IOException, InterruptedException {
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

		log.debug("{}: Running \"exec\" command: {}", containerName, String.join(" ", command));
		final ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
				.withAttachStdout(true).withAttachStderr(true).withCmd(command).withPrivileged(true).exec();

		final ToStringConsumer stdoutConsumer = new ToStringConsumer();
		final ToStringConsumer stderrConsumer = new ToStringConsumer();

		try (FrameConsumerResultCallback callback = new FrameConsumerResultCallback()) {
			callback.addConsumer(OutputFrame.OutputType.STDOUT, stdoutConsumer);
			callback.addConsumer(OutputFrame.OutputType.STDERR, stderrConsumer);

			dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(callback).awaitCompletion();
		}
		Integer exitCode = dockerClient.inspectExecCmd(execCreateCmdResponse.getId()).exec().getExitCode();

		final ExecResult result = new ExecResult(exitCode, stdoutConsumer.toString(StandardCharsets.UTF_8),
				stderrConsumer.toString(StandardCharsets.UTF_8));

		log.trace("{}: stdout: {}", containerName, result.getStdout());
		log.trace("{}: stderr: {}", containerName, result.getStderr());
		return result;
	}

	static class ExecResult {

		int exitCode;
		String stdout;
		String stderr;

		public ExecResult(int exitCode, String stdout, String stderr) {
			super();
			this.exitCode = exitCode;
			this.stdout = stdout;
			this.stderr = stderr;
		}

		public int getExitCode() {
			return exitCode;
		}

		public void setExitCode(int exitCode) {
			this.exitCode = exitCode;
		}

		public String getStdout() {
			return stdout;
		}

		public void setStdout(String stdout) {
			this.stdout = stdout;
		}

		public String getStderr() {
			return stderr;
		}

		public void setStderr(String stderr) {
			this.stderr = stderr;
		}

	}

	private boolean isRunning(InspectContainerResponse containerInfo) {
		try {
			return containerInfo != null && Boolean.TRUE.equals(containerInfo.getState().getRunning());
		} catch (DockerException e) {
			return false;
		}
	}

}

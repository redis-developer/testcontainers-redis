package com.redislabs.testcontainers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import com.redislabs.testcontainers.support.enterprise.DatabaseProvisioner;
import com.redislabs.testcontainers.support.enterprise.RestAPI;
import com.redislabs.testcontainers.support.enterprise.rest.Database;
import com.redislabs.testcontainers.support.enterprise.rest.DatabaseCreateResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.org.apache.commons.lang.ClassUtils;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestEnvironment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Slf4j
public class RedisEnterpriseContainer extends GenericContainer<RedisEnterpriseContainer> implements RedisServer {

    public static final String MODULE_BLOOM = "bf";
    public static final String MODULE_GEARS = "rg";
    public static final String MODULE_GRAPH = "graph";
    public static final String MODULE_JSON = "ReJSON";
    public static final String MODULE_SEARCH = "search";
    public static final String MODULE_TIMESERIES = "timeseries";

    public static final String ADMIN_USERNAME = "testcontainers@redislabs.com";
    public static final String ADMIN_PASSWORD = "redislabs123";
    public static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("redislabs/redis");
    private static final Duration DEFAULT_RLADMIN_WAIT_INTERVAL = Duration.ofMillis(500);
    private static final String NODE_EXTERNAL_ADDR = "0.0.0.0";
    private static final int DEFAULT_SHARD_COUNT = 2;
    private static final String DEFAULT_DATABASE_NAME = "testcontainers";
    private static final String RLADMIN = "/opt/redislabs/bin/rladmin";
    public static int ADMIN_PORT = 8443;
    public static int ENDPOINT_PORT = 12000;

    private DatabaseProvisioner.Options provisionerOptions = DatabaseProvisioner.Options.builder().build();
    private Duration rladminWaitDuration = DEFAULT_RLADMIN_WAIT_INTERVAL;
    private String databaseName = DEFAULT_DATABASE_NAME;
    private int shardCount = DEFAULT_SHARD_COUNT;
    private boolean ossCluster;
    private Database.Module[] modules = new Database.Module[0];
    private String restAPIHost = RestAPI.DEFAULT_HOST;

    public RedisEnterpriseContainer() {
        super(DEFAULT_IMAGE_NAME);
        addFixedExposedPort(RestAPI.DEFAULT_PORT, RestAPI.DEFAULT_PORT);
        addFixedExposedPort(ADMIN_PORT, ADMIN_PORT);
        addFixedExposedPort(ENDPOINT_PORT, ENDPOINT_PORT);
        withPrivilegedMode(true);
        withPublishAllPorts(false);
        waitingFor(Wait.forLogMessage(".*success: job_scheduler entered RUNNING state, process has stayed up for.*\\n", 1));
    }

    public RedisEnterpriseContainer withRestAPIHost(String host) {
        this.restAPIHost = host;
        return this;
    }

    public RedisEnterpriseContainer withProvisionerOptions(DatabaseProvisioner.Options options) {
        this.provisionerOptions = options;
        return this;
    }

    public RedisEnterpriseContainer withRladminWaitDuration(Duration rladminWaitDuration) {
        this.rladminWaitDuration = rladminWaitDuration;
        return this;
    }

    public RedisEnterpriseContainer withShardCount(int shardCount) {
        this.shardCount = shardCount;
        return this;
    }

    public RedisEnterpriseContainer withModules(Database.Module... modules) {
        this.modules = modules;
        return this;
    }

    public RedisEnterpriseContainer withDatabaseName(String name) {
        this.databaseName = name;
        return this;
    }

    @SneakyThrows
    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        super.containerIsStarted(containerInfo);
        log.info("Creating Redis Enterprise cluster");
        String username = ADMIN_USERNAME;
        String password = ADMIN_PASSWORD;
        execute(RLADMIN, "cluster", "create", "name", "cluster.local", "username", username, "password", password);
        Thread.sleep(rladminWaitDuration.toMillis());
        log.info("Disabling IPv6 support on Redis Enterprise cluster");
        execute(RLADMIN, "cluster", "config", "ipv6", "disabled");
        Thread.sleep(rladminWaitDuration.toMillis());
//        String externalAddress = NODE_EXTERNAL_ADDR;
//        log.info("Setting Redis Enterprise node external IP to {}", externalAddress);
//        execute(RLADMIN, "node", "1", "external_addr", "set", externalAddress);
//        Thread.sleep(rladminWaitDuration.toMillis());
        String host = getHost();
        log.info("Creating REST API client with username={}, password={}, host={}", username, password, host);
        RestAPI restAPI = RestAPI.credentials(new UsernamePasswordCredentials(username, password.toCharArray())).host(host).build();
        DatabaseProvisioner provisioner = DatabaseProvisioner.restAPI(restAPI).options(provisionerOptions).build();
        Database database = Database.name(databaseName).port(ENDPOINT_PORT).shardCount(shardCount).ossCluster(ossCluster).modules(modules).build();
        DatabaseCreateResponse response = provisioner.create(database);
        log.info("Created database {} with UID {}", database.getName(), response.getUid());
    }

    public RedisEnterpriseContainer withOSSCluster() {
        this.ossCluster = true;
        return this;
    }

    @Override
    public String getRedisURI() {
        return RedisServer.redisURI(getHost(), ENDPOINT_PORT);
    }

    @Override
    public boolean isCluster() {
        return ossCluster;
    }

    public String toString() {
        return ClassUtils.getShortClassName(this.getClass());
    }

    @SuppressWarnings("deprecation")
    public ExecResult execute(String... command) throws UnsupportedOperationException, IOException, InterruptedException {
        if (!TestEnvironment.dockerExecutionDriverSupportsExec()) {
            // at time of writing, this is the expected result in CircleCI.
            throw new UnsupportedOperationException("Your docker daemon is running the \"lxc\" driver, which doesn't support \"docker exec\".");
        }

        InspectContainerResponse containerInfo = getContainerInfo();
        if (!isRunning(containerInfo)) {
            throw new IllegalStateException("execInContainer can only be used while the Container is running");
        }

        String containerId = containerInfo.getId();
        String containerName = containerInfo.getName();

        DockerClient dockerClient = DockerClientFactory.instance().client();

        log.debug("{}: Running \"exec\" command: {}", containerName, String.join(" ", command));
        final ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId).withAttachStdout(true).withAttachStderr(true).withCmd(command).withPrivileged(true).exec();

        final ToStringConsumer stdoutConsumer = new ToStringConsumer();
        final ToStringConsumer stderrConsumer = new ToStringConsumer();

        try (FrameConsumerResultCallback callback = new FrameConsumerResultCallback()) {
            callback.addConsumer(OutputFrame.OutputType.STDOUT, stdoutConsumer);
            callback.addConsumer(OutputFrame.OutputType.STDERR, stderrConsumer);

            dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(callback).awaitCompletion();
        }
        Integer exitCode = dockerClient.inspectExecCmd(execCreateCmdResponse.getId()).exec().getExitCode();

        final ExecResult result = new ExecResult(
                exitCode,
                stdoutConsumer.toString(StandardCharsets.UTF_8),
                stderrConsumer.toString(StandardCharsets.UTF_8));

        log.trace("{}: stdout: {}", containerName, result.getStdout());
        log.trace("{}: stderr: {}", containerName, result.getStderr());
        return result;
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.MODULE)
    static class ExecResult {
        int exitCode;
        String stdout;
        String stderr;
    }

    private boolean isRunning(InspectContainerResponse containerInfo) {
        try {
            return containerInfo != null && Boolean.TRUE.equals(containerInfo.getState().getRunning());
        } catch (DockerException e) {
            return false;
        }
    }


}


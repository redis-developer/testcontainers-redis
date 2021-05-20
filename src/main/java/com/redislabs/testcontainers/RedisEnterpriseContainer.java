package com.redislabs.testcontainers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import com.redislabs.testcontainers.support.enterprise.ActionStatus;
import com.redislabs.testcontainers.support.enterprise.Command;
import com.redislabs.testcontainers.support.enterprise.CommandResponse;
import com.redislabs.testcontainers.support.enterprise.Database;
import com.redislabs.testcontainers.support.enterprise.DatabaseCreateResponse;
import com.redislabs.testcontainers.support.enterprise.Module;
import com.redislabs.testcontainers.support.enterprise.ModuleInstallResponse;
import com.redislabs.testcontainers.support.enterprise.RestAPI;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestEnvironment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"unused", "UnusedReturnValue", "BusyWait"})
@Slf4j
public class RedisEnterpriseContainer extends GenericContainer<RedisEnterpriseContainer> implements RedisServer {

    public static final String ADMIN_USERNAME = "testcontainers@redislabs.com";
    public static final String ADMIN_PASSWORD = "redislabs123";
    public static final String GEARS_MODULE_FILE = "redisgears.linux-bionic-x64.1.0.6.zip";
    public static final String URL_GEARS_MODULE = "https://redismodules.s3.amazonaws.com/redisgears/" + GEARS_MODULE_FILE;
    public static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("redislabs/redis");
    public static final List<Database.Regex> DEFAULT_SHARD_KEY_REGEX = Arrays.asList(Database.Regex.of(".*\\{(?<tag>.*)\\}.*"), Database.Regex.of("(?<tag>.*)"));
    private static final long DEFAULT_MODULE_INSTALLATION_TIMEOUT = 60000;
    private static final long DEFAULT_MODULE_INSTALLATION_CHECK_INTERVAL = 5000;
    private static final long DEFAULT_DATABASE_PING_TIMEOUT = 10000;
    private static final long DEFAULT_DATABASE_PING_RETRY_INTERVAL = 1000;
    private static final long DEFAULT_RLADMIN_WAIT_INTERVAL = 500;
    private static final String NODE_EXTERNAL_ADDR = "0.0.0.0";
    private static final int DEFAULT_CLUSTER_SHARD_COUNT = 3;
    public static int ADMIN_PORT = 8443;
    public static int ENDPOINT_PORT = 12000;
    private static final Map<String, RedisModule> MODULES = Stream.of(RedisModule.values()).collect(Collectors.toMap(RedisModule::getModuleName, Function.identity()));

    private final Set<RedisModule> modules = new HashSet<>();
    private RestAPI restAPI = RestAPI.credentials(new UsernamePasswordCredentials(ADMIN_USERNAME, ADMIN_PASSWORD.toCharArray())).build();
    private Duration moduleInstallationTimeout = Duration.ofMillis(DEFAULT_MODULE_INSTALLATION_TIMEOUT);
    private Duration moduleInstallationCheckInterval = Duration.ofMillis(DEFAULT_MODULE_INSTALLATION_CHECK_INTERVAL);
    private Duration databasePingTimeout = Duration.ofMillis(DEFAULT_DATABASE_PING_TIMEOUT);
    private Database database = Database.builder().name("testcontainers").build();
    private Duration databasePingRetryInterval = Duration.ofMillis(DEFAULT_DATABASE_PING_RETRY_INTERVAL);
    private Duration rladminWaitDuration = Duration.ofMillis(DEFAULT_RLADMIN_WAIT_INTERVAL);

    public RedisEnterpriseContainer() {
        super(DEFAULT_IMAGE_NAME);
        addFixedExposedPort(ADMIN_PORT, ADMIN_PORT);
        addFixedExposedPort(ENDPOINT_PORT, ENDPOINT_PORT);
        withPrivilegedMode(true);
        withPublishAllPorts(false);
        waitingFor(Wait.forLogMessage(".*success: job_scheduler entered RUNNING state, process has stayed up for.*\\n", 1));
    }

    public RedisEnterpriseContainer withRestAPI(RestAPI restAPI) {
        this.restAPI = restAPI;
        return this;
    }

    public RedisEnterpriseContainer withDatabase(Database database) {
        this.database = database;
        return this;
    }

    public RedisEnterpriseContainer withModuleInstallationCheckInterval(Duration moduleInstallationCheckInterval) {
        this.moduleInstallationCheckInterval = moduleInstallationCheckInterval;
        return this;
    }

    public RedisEnterpriseContainer withRladminWaitDuration(Duration rladminWaitDuration) {
        this.rladminWaitDuration = rladminWaitDuration;
        return this;
    }

    public RedisEnterpriseContainer withShardCount(int shardCount) {
        this.database.setShardCount(shardCount);
        if (shardCount > 1) {
            this.database.setSharding(true);
            this.database.setShardKeyRegex(DEFAULT_SHARD_KEY_REGEX);
        }
        return this;
    }

    public RedisEnterpriseContainer withModuleInstallationTimeout(Duration moduleInstallationTimeout) {
        this.moduleInstallationTimeout = moduleInstallationTimeout;
        return this;
    }

    public RedisEnterpriseContainer withDatabasePingTimeout(Duration databasePingTimeout) {
        this.databasePingTimeout = databasePingTimeout;
        return this;
    }

    public RedisEnterpriseContainer databasePingRetryInterval(Duration databasePingRetryInterval) {
        this.databasePingRetryInterval = databasePingRetryInterval;
        return this;
    }

    public RedisEnterpriseContainer withOSSCluster() {
        this.database.setOssCluster(true);
        this.database.setProxyPolicy(Database.ProxyPolicy.ALL_MASTER_SHARDS);
        this.database.setOssClusterAPIPreferredIPType(Database.IPType.EXTERNAL);
        if (database.getShardCount() == null || database.getShardCount() < 2) {
            log.info("Setting shard count to {}", DEFAULT_CLUSTER_SHARD_COUNT);
            withShardCount(DEFAULT_CLUSTER_SHARD_COUNT);
        }
        return this;
    }

    public RedisEnterpriseContainer withModules(RedisModule... modules) {
        Collections.addAll(this.modules, modules);
        return this;
    }

    @Override
    public void start() {
        addFixedExposedPort(restAPI.getPort(), restAPI.getPort());
        super.start();
    }

    @SneakyThrows
    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        super.containerIsStarted(containerInfo);
        execute("/opt/redislabs/bin/rladmin", "cluster", "create", "name", "cluster.local", "username", ADMIN_USERNAME, "password", ADMIN_PASSWORD);
        Thread.sleep(rladminWaitDuration.toMillis());
        execute("/opt/redislabs/bin/rladmin", "node", "1", "external_addr", "set", NODE_EXTERNAL_ADDR);
        Thread.sleep(rladminWaitDuration.toMillis());
        if (!modules.isEmpty()) {
            if (modules.contains(RedisModule.GEARS)) {
                installGears();
            }
            List<Module> availableModules = restAPI.modules();
            Map<RedisModule, String> moduleIds = availableModules.stream().collect(Collectors.toMap(m -> MODULES.get(m.getName()), Module::getId));
            database.setModuleList(modules.stream().map(m -> Database.Module.builder().id(moduleIds.get(m)).name(m.getModuleName()).build()).collect(Collectors.toList()));
        }
        DatabaseCreateResponse databaseCreateResponse = restAPI.create(database);
        if (canPingDatabase(databaseCreateResponse.getUid())) {
            log.info("Created database {} with UID {}", databaseCreateResponse.getName(), databaseCreateResponse.getUid());
        } else {
            throw new ContainerLaunchException("Could not ping database at " + getRedisURI());
        }
    }


    private boolean canPingDatabase(long uid) throws Exception {
        long start = System.currentTimeMillis();
        do {
            try {
                CommandResponse response = restAPI.command(uid, Command.command("PING").build());
                if (response.getResponse().asBoolean()) {
                    return true;
                }
            } catch (Exception e) {
                // ignore
            }
            Thread.sleep(databasePingRetryInterval.toMillis());
        } while (System.currentTimeMillis() - start < databasePingTimeout.toMillis());
        return false;
    }

    private void installGears() throws Exception {
        try (InputStream zipInputStream = new URL(URL_GEARS_MODULE).openStream()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(zipInputStream, baos);
            ModuleInstallResponse installResponse = restAPI.module(GEARS_MODULE_FILE, baos.toByteArray());
            long start = System.currentTimeMillis();
            ActionStatus status;
            do {
                status = restAPI.actionStatus(installResponse.getActionUID());
                if ("completed".equals(status.getStatus())) {
                    return;
                }
                // Wait before checking again
                Thread.sleep(moduleInstallationCheckInterval.toMillis());
            } while (System.currentTimeMillis() - start < moduleInstallationTimeout.toMillis());
            throw new ContainerLaunchException("Timed out waiting for module installation to complete. Action UID: " + installResponse.getActionUID());
        }
    }

    @Override
    public String getRedisURI() {
        return RedisServer.redisURI(getHost(), ENDPOINT_PORT);
    }

    @Override
    public boolean isCluster() {
        return Boolean.TRUE.equals(database.getOssCluster());
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

    public enum RedisModule {

        JSON("ReJSON"), SEARCH("search"), TIMESERIES("timeseries"), BLOOM("bf"), GRAPH("graph"), GEARS("rg");

        @Getter
        private final String moduleName;

        RedisModule(String name) {
            this.moduleName = name;
        }

    }

}


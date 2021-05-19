package com.redislabs.testcontainers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.classic.methods.HttpGet;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.classic.methods.HttpPost;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.entity.mime.ByteArrayBody;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.impl.auth.BasicScheme;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.impl.classic.HttpClients;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.io.HttpClientConnectionManager;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.protocol.HttpClientContext;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.ssl.TrustAllStrategy;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.ClassicHttpRequest;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.ContentType;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpException;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpHeaders;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpHost;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.io.entity.EntityUtils;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.io.entity.StringEntity;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.ssl.SSLContexts;
import com.redislabs.testcontainers.support.enterprise.ActionStatus;
import com.redislabs.testcontainers.support.enterprise.Database;
import com.redislabs.testcontainers.support.enterprise.DatabaseCreateResponse;
import com.redislabs.testcontainers.support.enterprise.ModuleInstallResponse;
import com.redislabs.testcontainers.support.enterprise.ModuleResponse;
import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
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
import org.testcontainers.shaded.com.fasterxml.jackson.databind.DeserializationFeature;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestEnvironment;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"unused", "UnusedReturnValue", "BusyWait"})
@Slf4j
public class RedisEnterpriseContainer extends GenericContainer<RedisEnterpriseContainer> implements RedisServer {

    public static final String GEARS_MODULE_FILE = "redisgears.linux-bionic-x64.1.0.6.zip";
    public static final String URL_GEARS_MODULE = "https://redismodules.s3.amazonaws.com/redisgears/" + GEARS_MODULE_FILE;
    public static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("redislabs/redis");
    public static final List<Database.Regex> DEFAULT_SHARD_KEY_REGEX = Arrays.asList(Database.Regex.of(".*\\{(?<tag>.*)\\}.*"), Database.Regex.of("(?<tag>.*)"));
    private static final String API_V1 = "/v1/";
    private static final String API_V2 = "/v2/";
    private static final int DEFAULT_REST_REQUEST_MAX_RETRIES = 10;
    private static final long DEFAULT_REST_REQUEST_RETRY_INTERVAL = 3000;
    private static final long DEFAULT_MODULE_INSTALLATION_TIMEOUT = 60000;
    private static final long DEFAULT_MODULE_INSTALLATION_CHECK_INTERVAL = 5000;
    private static final long DEFAULT_DATABASE_PING_TIMEOUT = 10000;
    private static final long DEFAULT_DATABASE_PING_RETRY_INTERVAL = 1000;
    private static final long DEFAULT_RLADMIN_WAIT_INTERVAL = 500;
    private static final String NODE_EXTERNAL_ADDR = "0.0.0.0";
    private static final int DEFAULT_CLUSTER_SHARD_COUNT = 3;
    public static int ADMIN_PORT = 8443;
    public static int REST_PORT = 9443;
    public static int ENDPOINT_PORT = 12000;
    public static final String REST_PROTOCOL = "https";
    public static final String REST_HOST = "localhost";
    public static final String REST_ACTIONS = "actions";
    public static final String REST_MODULES = "modules";
    public static final String REST_BDBS = "bdbs";
    private static final String ADMIN_USERNAME = "testcontainers@redislabs.com";
    private static final String ADMIN_PASSWORD = "redislabs123";
    private static final Object CONTENT_TYPE_JSON = "application/json";
    private static final Map<String, RedisModule> MODULES = Stream.of(RedisModule.values()).collect(Collectors.toMap(RedisModule::getModuleName, Function.identity()));

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Set<RedisModule> modules = new HashSet<>();
    private Duration moduleInstallationTimeout = Duration.ofMillis(DEFAULT_MODULE_INSTALLATION_TIMEOUT);
    private Duration moduleInstallationCheckInterval = Duration.ofMillis(DEFAULT_MODULE_INSTALLATION_CHECK_INTERVAL);
    private Duration databasePingTimeout = Duration.ofMillis(DEFAULT_DATABASE_PING_TIMEOUT);
    private Database database = Database.builder().name("testcontainers").build();
    private Duration databasePingRetryInterval = Duration.ofMillis(DEFAULT_DATABASE_PING_RETRY_INTERVAL);
    private Duration rladminWaitDuration = Duration.ofMillis(DEFAULT_RLADMIN_WAIT_INTERVAL);
    private Duration restRequestRetryInterval = Duration.ofMillis(DEFAULT_REST_REQUEST_RETRY_INTERVAL);
    private int restRequestMaxRetries = DEFAULT_REST_REQUEST_MAX_RETRIES;

    public RedisEnterpriseContainer() {
        super(DEFAULT_IMAGE_NAME);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        addFixedExposedPort(ADMIN_PORT, ADMIN_PORT);
        addFixedExposedPort(REST_PORT, REST_PORT);
        addFixedExposedPort(ENDPOINT_PORT, ENDPOINT_PORT);
        withPrivilegedMode(true);
        withPublishAllPorts(false);
        waitingFor(Wait.forLogMessage(".*success: job_scheduler entered RUNNING state, process has stayed up for.*\\n", 1));
    }

    public RedisEnterpriseContainer withRestRequestRetryInterval(Duration restRequestRetryInterval) {
        this.restRequestRetryInterval = restRequestRetryInterval;
        return this;
    }

    public RedisEnterpriseContainer withRestRequestMaxRetries(int restRequestMaxRetries) {
        this.restRequestMaxRetries = restRequestMaxRetries;
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
            ModuleResponse[] availableModules = read(new HttpGet(toV1URI(REST_MODULES)), ModuleResponse[].class);
            Map<RedisModule, String> moduleIds = Stream.of(availableModules).collect(Collectors.toMap(m -> MODULES.get(m.getName()), ModuleResponse::getId));
            database.setModuleList(modules.stream().map(m -> Database.Module.builder().id(moduleIds.get(m)).name(m.getModuleName()).build()).collect(Collectors.toList()));
        }
        HttpPost request = new HttpPost(toV1URI(REST_BDBS));
        String json = objectMapper.writeValueAsString(database);
        log.info("Creating database: {}", json);
        request.setEntity(new StringEntity(json));
        DatabaseCreateResponse databaseCreateResponse = read(request, DatabaseCreateResponse.class);
        if (canPingDatabase()) {
            log.info("Created database {} with UID {}", databaseCreateResponse.getName(), databaseCreateResponse.getUid());
        } else {
            throw new ContainerLaunchException("Could not ping database at " + getRedisURI());
        }
    }

    private <C extends StatefulConnection<String, String>> boolean canPingDatabase() throws InterruptedException {
        AbstractRedisClient client;
        Supplier<StatefulConnection<String, String>> connectionSupplier;
        Function<StatefulConnection<String, String>, String> pingFunction;
        if (Boolean.TRUE.equals(database.getOssCluster())) {
            client = RedisClusterClient.create(getRedisURI());
            connectionSupplier = ((RedisClusterClient) client)::connect;
            pingFunction = c -> ((StatefulRedisClusterConnection<String, String>) c).sync().ping();
        } else {
            client = RedisClient.create(getRedisURI());
            connectionSupplier = ((RedisClient) client)::connect;
            pingFunction = c -> ((StatefulRedisConnection<String, String>) c).sync().ping();
        }
        try {
            long start = System.currentTimeMillis();
            do {
                try (StatefulConnection<String, String> connection = connectionSupplier.get()) {
                    if ("PONG".equals(pingFunction.apply(connection))) {
                        return true;
                    }
                } catch (Exception e) {
                    // ignore
                }
                Thread.sleep(databasePingRetryInterval.toMillis());
            } while (System.currentTimeMillis() - start < databasePingTimeout.toMillis());
            return false;
        } finally {
            client.shutdown();
            client.getResources().shutdown();
        }
    }

    private void installGears() throws IOException, URISyntaxException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, HttpException, InterruptedException {
        HttpPost post = new HttpPost(toV2URI(REST_MODULES));
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.STRICT);
        try (InputStream zipInputStream = new URL(URL_GEARS_MODULE).openStream()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(zipInputStream, baos);
            builder.addPart("module", new ByteArrayBody(baos.toByteArray(), ContentType.MULTIPART_FORM_DATA, GEARS_MODULE_FILE));
            post.setEntity(builder.build());
            ModuleInstallResponse installResponse = read(post, ModuleInstallResponse.class, HttpStatus.SC_ACCEPTED);
            long start = System.currentTimeMillis();
            ActionStatus status;
            do {
                status = read(new HttpGet(toV1URI(REST_ACTIONS, installResponse.getActionUID())), ActionStatus.class);
                if ("completed".equals(status.getStatus())) {
                    return;
                }
                // Wait before checking again
                Thread.sleep(moduleInstallationCheckInterval.toMillis());
            } while (System.currentTimeMillis() - start < moduleInstallationTimeout.toMillis());
            throw new ContainerLaunchException("Timed out waiting for module installation to complete. Action UID: " + installResponse.getActionUID());
        }
    }

    private URI toV2URI(String... segments) throws URISyntaxException {
        return toURI(API_V2, segments);
    }

    private URI toV1URI(String... segments) throws URISyntaxException {
        return toURI(API_V1, segments);
    }

    private URI toURI(String prefix, String... segments) throws URISyntaxException {
        return new URI(REST_PROTOCOL, null, REST_HOST, REST_PORT, prefix + String.join("/", segments), null, null);
    }

    @Override
    public String getRedisURI() {
        return RedisServer.redisURI(getHost(), ENDPOINT_PORT);
    }

    @Override
    public boolean isCluster() {
        return Boolean.TRUE.equals(database.getOssCluster());
    }

    private <T> T read(ClassicHttpRequest request, Class<T> clazz) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, HttpException, InterruptedException {
        request.setHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON);
        return read(request, clazz, HttpStatus.SC_OK);
    }

    private <T> T read(ClassicHttpRequest request, Class<T> clazz, int successCode) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, HttpException, InterruptedException {
        try (CloseableHttpClient client = client()) {
            int retries = 0;
            CloseableHttpResponse response;
            do {
                response = execute(request, client);
                if (response.getCode() == successCode) {
                    return objectMapper.readValue(EntityUtils.toString(response.getEntity()), clazz);
                }
                Thread.sleep(restRequestRetryInterval.toMillis());
                retries++;
            } while (retries < restRequestMaxRetries);
            throw new HttpException("Redis Enterprise REST API responded with " + response);
        }
    }

    private CloseableHttpResponse execute(ClassicHttpRequest request, CloseableHttpClient client) throws IOException {
        BasicScheme basicAuth = new BasicScheme();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(ADMIN_USERNAME, ADMIN_PASSWORD.toCharArray());
        basicAuth.initPreemptive(credentials);
        HttpHost target = new HttpHost(REST_PROTOCOL, REST_HOST, REST_PORT);
        HttpClientContext localContext = HttpClientContext.create();
        localContext.resetAuthExchange(target, basicAuth);
        return client.execute(request, localContext);
    }

    private CloseableHttpClient client() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        SSLContext sslcontext = SSLContexts.custom().loadTrustMaterial(new TrustAllStrategy()).build();
        SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create().setSslContext(sslcontext).setHostnameVerifier(NoopHostnameVerifier.INSTANCE).build();
        HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create().setSSLSocketFactory(sslSocketFactory).build();
        return HttpClients.custom().setConnectionManager(cm).build();
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


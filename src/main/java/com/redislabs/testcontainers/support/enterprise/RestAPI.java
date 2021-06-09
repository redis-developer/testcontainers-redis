package com.redislabs.testcontainers.support.enterprise;

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
import com.redislabs.testcontainers.support.enterprise.rest.ActionStatus;
import com.redislabs.testcontainers.support.enterprise.rest.Command;
import com.redislabs.testcontainers.support.enterprise.rest.CommandResponse;
import com.redislabs.testcontainers.support.enterprise.rest.Database;
import com.redislabs.testcontainers.support.enterprise.rest.DatabaseCreateResponse;
import com.redislabs.testcontainers.support.enterprise.rest.Module;
import com.redislabs.testcontainers.support.enterprise.rest.ModuleInstallResponse;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.DeserializationFeature;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JavaType;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.type.SimpleType;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;


@Slf4j
@Data
@Builder
public class RestAPI {

    public static final Object CONTENT_TYPE_JSON = "application/json";
    public static final String V1 = "/v1/";
    public static final String V2 = "/v2/";
    public static final String DEFAULT_PROTOCOL = "https";
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 9443;
    public static final String ACTIONS = "actions";
    public static final String MODULES = "modules";
    public static final String BDBS = "bdbs";
    public static final String COMMAND = "command";
    private static final CharSequence PATH_SEPARATOR = "/";

    @Builder.Default
    private String protocol = DEFAULT_PROTOCOL;
    @Builder.Default
    private String host = DEFAULT_HOST;
    @Builder.Default
    private int port = DEFAULT_PORT;
    private final UsernamePasswordCredentials credentials;
    private final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static String v1(String... segments) {
        return join(V1, segments);
    }

    private static String v2(String... segments) {
        return join(V2, segments);
    }

    private static String join(String path, String[] segments) {
        return path + String.join(PATH_SEPARATOR, segments);
    }

    public static RestAPIBuilder credentials(UsernamePasswordCredentials credentials) {
        return new RestAPIBuilder().credentials(credentials);
    }

    private URI uri(String path) throws URISyntaxException {
        return new URI(protocol, null, host, port, path, null, null);
    }

    private <T> T get(String path, Class<T> type) throws Exception {
        return get(path, SimpleType.constructUnsafe(type));
    }

    private <T> T get(String path, JavaType type) throws Exception {
        return read(new HttpGet(uri(path)), type);
    }

    private <T> T post(String path, Object request, Class<T> responseType) throws Exception {
        return post(path, request, SimpleType.constructUnsafe(responseType));
    }

    private <T> T post(String path, Object request, JavaType responseType) throws Exception {
        HttpPost post = new HttpPost(uri(path));
        String json = objectMapper.writeValueAsString(request);
        post.setEntity(new StringEntity(json));
        log.info("POST {}", json);
        return read(post, responseType);
    }

    private <T> T read(ClassicHttpRequest request, JavaType type) throws Exception {
        request.setHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON);
        return read(request, type, HttpStatus.SC_OK);
    }

    private <T> T read(ClassicHttpRequest request, Class<T> type, int successCode) throws Exception {
        return read(request, SimpleType.constructUnsafe(type), successCode);
    }

    private <T> T read(ClassicHttpRequest request, JavaType type, int successCode) throws Exception {
        try (CloseableHttpClient client = client()) {
            CloseableHttpResponse response = execute(request, client);
            if (response.getCode() == successCode) {
                return objectMapper.readValue(EntityUtils.toString(response.getEntity()), type);
            }
            throw new HttpException(response.getCode() + ": " + EntityUtils.toString(response.getEntity()));
        }
    }

    private CloseableHttpResponse execute(ClassicHttpRequest request, CloseableHttpClient client) throws IOException {
        BasicScheme basicAuth = new BasicScheme();
        basicAuth.initPreemptive(credentials);
        HttpHost target = new HttpHost(protocol, host, port);
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

    public List<Module> modules() throws Exception {
        return get(v1(MODULES), objectMapper.getTypeFactory().constructCollectionType(List.class, Module.class));
    }

    public DatabaseCreateResponse create(Database database) throws Exception {
        return post(v1(BDBS), database, DatabaseCreateResponse.class);
    }

    public ModuleInstallResponse module(String filename, byte[] bytes) throws Exception {
        HttpPost post = new HttpPost(uri(v2(MODULES)));
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.STRICT);
        builder.addPart("module", new ByteArrayBody(bytes, ContentType.MULTIPART_FORM_DATA, filename));
        post.setEntity(builder.build());
        return read(post, ModuleInstallResponse.class, HttpStatus.SC_ACCEPTED);
    }

    public ActionStatus actionStatus(String actionUID) throws Exception {
        return get(v1(ACTIONS, actionUID), ActionStatus.class);
    }

    public CommandResponse command(long bdb, Command command) throws Exception {
        return post(v1(BDBS, String.valueOf(bdb), COMMAND), command, CommandResponse.class);
    }

}

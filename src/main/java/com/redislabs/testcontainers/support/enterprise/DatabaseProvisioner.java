package com.redislabs.testcontainers.support.enterprise;

import com.redislabs.testcontainers.RedisEnterpriseContainer;
import com.redislabs.testcontainers.support.enterprise.rest.ActionStatus;
import com.redislabs.testcontainers.support.enterprise.rest.Command;
import com.redislabs.testcontainers.support.enterprise.rest.CommandResponse;
import com.redislabs.testcontainers.support.enterprise.rest.Database;
import com.redislabs.testcontainers.support.enterprise.rest.DatabaseCreateResponse;
import com.redislabs.testcontainers.support.enterprise.rest.Module;
import com.redislabs.testcontainers.support.enterprise.rest.ModuleInstallResponse;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("BusyWait")
@Slf4j
public class DatabaseProvisioner {

    public static final String GEARS_MODULE_FILE = "redisgears.linux-bionic-x64.1.0.6.zip";
    public static final String URL_GEARS_MODULE = "https://redismodules.s3.amazonaws.com/redisgears/" + GEARS_MODULE_FILE;

    private final RestAPI restAPI;
    private final Options options;

    public DatabaseProvisioner(RestAPI restAPI, Options options) {
        this.restAPI = restAPI;
        this.options = options;
    }

    public static DatabaseProvisionerBuilder restAPI(RestAPI restAPI) {
        return new DatabaseProvisionerBuilder(restAPI);
    }

    @Setter
    @Accessors(fluent = true)
    public static class DatabaseProvisionerBuilder {


        private final RestAPI restAPI;
        private Options options = Options.builder().build();

        public DatabaseProvisionerBuilder(RestAPI restAPI) {
            this.restAPI = restAPI;
        }

        public DatabaseProvisioner build() {
            return new DatabaseProvisioner(restAPI, options);
        }
    }

    public DatabaseCreateResponse create(Database database) throws Exception {
        if (!database.getModuleConfigs().isEmpty()) {
            Map<String, String> moduleIds = availableModules();
            for (Database.ModuleConfig moduleConfig : database.getModuleConfigs()) {
                if (!moduleIds.containsKey(moduleConfig.getName())) {
                    log.info("Module {} not installed", moduleConfig.getName());
                    if (RedisEnterpriseContainer.MODULE_GEARS.equals(moduleConfig.getName())) {
                        installGears();
                        moduleIds = availableModules();
                    }
                }
                moduleConfig.setId(moduleIds.get(moduleConfig.getName()));
            }
        }
        log.info("Creating database: {}", database);
        DatabaseCreateResponse databaseCreateResponse = restAPI.create(database);
        long uid = databaseCreateResponse.getUid();
        long start = System.currentTimeMillis();
        do {
            log.info("Pinging database {}", uid);
            try {
                CommandResponse response = restAPI.command(uid, Command.command("PING").build());
                if (response.getResponse().asBoolean()) {
                    return databaseCreateResponse;
                }
            } catch (Exception e) {
                // ignore
            }
            Thread.sleep(options.getPing().getInterval().toMillis());
        } while (System.currentTimeMillis() - start < options.getPing().getTimeout().toMillis());
        throw new TimeoutException("Timeout waiting for database " + databaseCreateResponse.getUid() + " to be ready");
    }

    private Map<String, String> availableModules() throws Exception {
        Map<String, String> moduleMap = new HashMap<>();
        for (Module module : restAPI.modules()) {
            moduleMap.put(module.getName(), module.getId());
        }
        return moduleMap;
    }

    private void installGears() throws Exception {
        URL url = new URL(URL_GEARS_MODULE);
        log.info("Downloading module file {}", url);
        try (InputStream zipInputStream = url.openStream()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(zipInputStream, baos);
            log.info("Installing module {}", GEARS_MODULE_FILE);
            ModuleInstallResponse installResponse = restAPI.module(GEARS_MODULE_FILE, baos.toByteArray());
            long start = System.currentTimeMillis();
            ActionStatus status;
            do {
                log.info("Checking status of action {}", installResponse.getActionUID());
                status = restAPI.actionStatus(installResponse.getActionUID());
                if ("completed".equals(status.getStatus())) {
                    log.info("Action {} completed", installResponse.getActionUID());
                    return;
                }
                log.info("Action {} {}", installResponse.getActionUID(), status.getStatus());
                // Wait before checking again
                Thread.sleep(options.getModuleInstallation().getCheckInterval().toMillis());
            } while (System.currentTimeMillis() - start < options.getModuleInstallation().getTimeout().toMillis());
            throw new ContainerLaunchException("Timed out waiting for module installation to complete. Action UID: " + installResponse.getActionUID());
        }
    }

    @Data
    @Builder
    public static class Options {
        @Builder.Default
        private PingOptions ping = PingOptions.builder().build();
        @Builder.Default
        private ModuleInstallationOptions moduleInstallation = ModuleInstallationOptions.builder().build();
    }

    @Data
    @Builder
    public static class PingOptions {
        public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
        public static final Duration DEFAULT_INTERVAL = Duration.ofSeconds(1);

        @Builder.Default
        private Duration timeout = DEFAULT_TIMEOUT;
        @Builder.Default
        private Duration interval = DEFAULT_INTERVAL;

    }

    @Data
    @Builder
    public static class ModuleInstallationOptions {

        public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(3);
        public static final Duration DEFAULT_CHECK_INTERVAL = Duration.ofSeconds(5);

        @Builder.Default
        private Duration timeout = DEFAULT_TIMEOUT;
        @Builder.Default
        private Duration checkInterval = DEFAULT_CHECK_INTERVAL;

    }
}

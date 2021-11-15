package com.redis.testcontainers.support.enterprise;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import com.redis.testcontainers.RedisEnterpriseContainer;
import com.redis.testcontainers.support.RetryCallable;
import com.redis.testcontainers.support.enterprise.rest.ActionResponse;
import com.redis.testcontainers.support.enterprise.rest.Command;
import com.redis.testcontainers.support.enterprise.rest.CommandResponse;
import com.redis.testcontainers.support.enterprise.rest.DatabaseCreateRequest;
import com.redis.testcontainers.support.enterprise.rest.DatabaseCreateRequest.ModuleConfig;
import com.redis.testcontainers.support.enterprise.rest.DatabaseCreateResponse;
import com.redis.testcontainers.support.enterprise.rest.ModuleResponse;

public class Provisioner {

	private static final Logger log = LoggerFactory.getLogger(Provisioner.class);

	public static final String GEARS_MODULE_FILE = "redisgears.linux-bionic-x64.1.0.6.zip";
	public static final Duration DEFAULT_PING_TIMEOUT = Duration.ofSeconds(10);
	public static final Duration DEFAULT_PING_INTERVAL = Duration.ofSeconds(1);
	public static final Duration DEFAULT_MODULE_INSTALLATION_TIMEOUT = Duration.ofMinutes(3);
	public static final Duration DEFAULT_MODULE_INSTALLATION_CHECK_INTERVAL = Duration.ofSeconds(5);

	private RestAPI restAPI = new RestAPI();

	private Duration pingTimeout = DEFAULT_PING_TIMEOUT;
	private Duration pingInterval = DEFAULT_PING_INTERVAL;
	private Duration moduleInstallationTimeout = DEFAULT_MODULE_INSTALLATION_TIMEOUT;
	private Duration moduleInstallationCheckInterval = DEFAULT_MODULE_INSTALLATION_CHECK_INTERVAL;

	public Provisioner withPingTimeout(Duration pingTimeout) {
		this.pingTimeout = pingTimeout;
		return this;
	}

	public Provisioner withPingInterval(Duration pingInterval) {
		this.pingInterval = pingInterval;
		return this;
	}

	public Provisioner withModuleInstallationTimeout(Duration moduleInstallationTimeout) {
		this.moduleInstallationTimeout = moduleInstallationTimeout;
		return this;
	}

	public Provisioner withModuleInstallationCheckInterval(Duration moduleInstallationCheckInterval) {
		this.moduleInstallationCheckInterval = moduleInstallationCheckInterval;
		return this;
	}

	public Provisioner withRestAPI(RestAPI restAPI) {
		this.restAPI = restAPI;
		return this;
	}

	public DatabaseCreateResponse create(DatabaseCreateRequest database) throws Exception {
		if (!database.getModuleConfigs().isEmpty()) {
			Map<String, String> moduleIds = availableModules();
			for (ModuleConfig moduleConfig : database.getModuleConfigs()) {
				if (!moduleIds.containsKey(moduleConfig.getName())) {
					log.info("Module {} not installed", moduleConfig.getName());
					if (RedisEnterpriseContainer.RedisModule.GEARS.getName().equals(moduleConfig.getName())) {
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
		return RetryCallable.delegate(() -> {
			log.info("Pinging database {}", uid);
			CommandResponse response = restAPI.command(uid, Command.of("PING"));
			if (response.getResponse().asBoolean()) {
				return databaseCreateResponse;
			}
			throw new Exception("Database not ready");
		}).sleep(pingInterval).timeout(pingTimeout).call();
	}

	private Map<String, String> availableModules() throws Exception {
		Map<String, String> moduleMap = new HashMap<>();
		for (ModuleResponse module : restAPI.modules()) {
			moduleMap.put(module.getName(), module.getId());
		}
		return moduleMap;
	}

	private void installGears() throws Exception {
		try (InputStream zipInputStream = getClass().getClassLoader().getResourceAsStream(GEARS_MODULE_FILE)) {
			if (zipInputStream == null) {
				throw new ContainerLaunchException(
						String.format("Could not find RedisGears module file '%s' in classpath", GEARS_MODULE_FILE));
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			IOUtils.copy(zipInputStream, baos);
			log.info("Installing module {}", GEARS_MODULE_FILE);
			String actionUID = restAPI.module(GEARS_MODULE_FILE, baos.toByteArray()).getActionUID();
			RetryCallable.delegate(() -> {
				log.info("Checking status of action {}", actionUID);
				ActionResponse status = restAPI.actionStatus(actionUID);
				if ("completed".equals(status.getStatus())) {
					log.info("Action {} completed", actionUID);
					return status;
				}
				log.info("Action {} {}", actionUID, status.getStatus());
				throw new ContainerLaunchException(
						"Timed out waiting for module installation to complete. Action UID: " + actionUID);
			}).sleep(moduleInstallationCheckInterval).timeout(moduleInstallationTimeout).call();
		}
	}

}

package com.redis.testcontainers;

import java.util.Objects;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.org.apache.commons.lang.ClassUtils;
import org.testcontainers.utility.DockerImageName;

public class RedisClusterContainer extends GenericContainer<RedisClusterContainer> implements RedisServer {

	public static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("grokzen/redis-cluster");
	public static final String DEFAULT_TAG = "6.2.1";
	public static final int DEFAULT_INITIAL_PORT = 7000;
	public static final int DEFAULT_MASTERS = 3;
	public static final int DEFAULT_SLAVES_PER_MASTER = 0;
	public static final String DEFAULT_IP = "0.0.0.0";
	public static final String ENV_SKIP_TESTS = "skipRedisClusterTests";

	private static final String ENV_MASTERS = "MASTERS";
	private static final String ENV_SLAVES_PER_MASTER = "SLAVES_PER_MASTER";
	private static final String ENV_INITIAL_PORT = "INITIAL_PORT";
	private static final String ENV_IP = "IP";
	private static final String KEYSPACE_NOTIFICATIONS_IMAGE_NAME = "jruaux/redis-cluster:6.2.1";

	private int initialPort = DEFAULT_INITIAL_PORT;
	private int masters = DEFAULT_MASTERS;
	private int slavesPerMaster = DEFAULT_SLAVES_PER_MASTER;

	/**
	 * @deprecated use {@link RedisClusterContainer(DockerImageName)} instead
	 */
	@Deprecated
	public RedisClusterContainer() {
		this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
	}

	public RedisClusterContainer(DockerImageName dockerImageName) {
		super(dockerImageName);
		withIP(DEFAULT_IP);
		update();
		waitingFor(Wait.forLogMessage(".*Cluster state changed: ok*\\n", 1));
	}

	@Override
	public boolean isCluster() {
		return true;
	}

	public RedisClusterContainer withKeyspaceNotifications() {
		setDockerImageName(KEYSPACE_NOTIFICATIONS_IMAGE_NAME);
		return this;
	}

	public RedisClusterContainer withIP(String ip) {
		withEnv(ENV_IP, ip);
		return this;
	}

	private RedisClusterContainer update() {
		withEnv(ENV_INITIAL_PORT, String.valueOf(initialPort));
		withEnv(ENV_MASTERS, String.valueOf(masters));
		withEnv(ENV_SLAVES_PER_MASTER, String.valueOf(slavesPerMaster));
		for (int port : ports()) {
			addFixedExposedPort(port, port);
		}
		return this;
	}

	private int[] ports() {
		int totalNodes = masters * (slavesPerMaster + 1);
		int[] ports = new int[totalNodes];
		for (int index = 0; index < totalNodes; index++) {
			ports[index] = initialPort + index;
		}
		return ports;
	}

	public String[] getRedisURIs() {
		int[] ports = ports();
		String[] redisURIs = new String[ports.length];
		for (int index = 0; index < ports.length; index++) {
			redisURIs[index] = RedisServer.redisURI(getHost(), ports[index]);
		}
		return redisURIs;
	}

	public String getRedisURI() {
		return getRedisURIs()[0];
	}

	public RedisClusterContainer withMasters(int count) {
		if (count <= 0) {
			throw new IllegalArgumentException("Count must be greater than zero");
		}
		this.masters = count;
		return update();
	}

	public RedisClusterContainer withSlavesPerMaster(int count) {
		if (count < 0) {
			throw new IllegalArgumentException("Count must be zero or greater");
		}
		this.slavesPerMaster = count;
		return update();
	}

	public RedisClusterContainer withInitialPort(int port) {
		this.initialPort = port;
		return update();
	}

	@Override
	public String toString() {
		return ClassUtils.getShortClassName(getClass()) + " active=" + isActive();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(initialPort, masters, slavesPerMaster);
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
		RedisClusterContainer other = (RedisClusterContainer) obj;
		return initialPort == other.initialPort && masters == other.masters && slavesPerMaster == other.slavesPerMaster;
	}

	@Override
	public boolean isActive() {
		return AbstractRedisContainer.isActive(ENV_SKIP_TESTS);
	}

}

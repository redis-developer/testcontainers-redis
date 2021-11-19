package com.redis.testcontainers.junit.jupiter;

import com.redis.lettucemod.RedisModulesClient;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.lettucemod.api.async.RedisModulesAsyncCommands;
import com.redis.lettucemod.api.reactive.RedisModulesReactiveCommands;
import com.redis.lettucemod.api.sync.RedisModulesCommands;
import com.redis.lettucemod.cluster.RedisModulesClusterClient;
import com.redis.testcontainers.RedisServer;

import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.RedisClient;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

public class RedisTestContext implements AutoCloseable {

	private final RedisServer server;
	private AbstractRedisClient client;
	private StatefulRedisModulesConnection<String, String> connection;
	private StatefulRedisPubSubConnection<String, String> pubSubConnection;

	public RedisTestContext(RedisServer server) {
		this.server = server;
	}

	@Override
	public String toString() {
		return server.toString();
	}

	public RedisServer getServer() {
		return server;
	}

	public AbstractRedisClient getClient() {
		if (client == null) {
			String uri = server.getRedisURI();
			client = server.isCluster() ? RedisModulesClusterClient.create(uri) : RedisModulesClient.create(uri);
		}
		return client;
	}

	public StatefulRedisModulesConnection<String, String> getConnection() {
		if (connection == null) {
			connection = connection();
		}
		return connection;
	}

	public StatefulRedisPubSubConnection<String, String> getPubSubConnection() {
		if (pubSubConnection == null) {
			pubSubConnection = pubSubConnection();
		}
		return pubSubConnection;
	}

	private StatefulRedisModulesConnection<String, String> connection() {
		if (server.isCluster()) {
			return ((RedisModulesClusterClient) getClient()).connect();
		}
		return ((RedisModulesClient) getClient()).connect();
	}

	private StatefulRedisPubSubConnection<String, String> pubSubConnection() {
		if (server.isCluster()) {
			return ((RedisModulesClusterClient) getClient()).connectPubSub();
		}
		return ((RedisModulesClient) getClient()).connectPubSub();
	}

	public RedisModulesCommands<String, String> sync() {
		return getConnection().sync();
	}

	public RedisModulesAsyncCommands<String, String> async() {
		return getConnection().async();
	}

	public RedisModulesReactiveCommands<String, String> reactive() {
		return getConnection().reactive();
	}

	@Override
	public void close() {
		if (pubSubConnection != null) {
			pubSubConnection.close();
			pubSubConnection = null;
		}
		if (connection != null) {
			connection.close();
			connection = null;
		}
		if (client != null) {
			client.shutdown();
			client.getResources().shutdown();
			client = null;
		}
	}

	public RedisClient getRedisClient() {
		return (RedisClient) getClient();
	}

	public RedisClusterClient getRedisClusterClient() {
		return (RedisClusterClient) getClient();
	}

	public boolean isCluster() {
		return server.isCluster();
	}

}

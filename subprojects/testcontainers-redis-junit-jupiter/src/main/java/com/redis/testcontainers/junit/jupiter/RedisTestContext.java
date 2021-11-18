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
	private final AbstractRedisClient client;
	private StatefulRedisModulesConnection<String, String> connection;
	private StatefulRedisPubSubConnection<String, String> pubSubConnection;

	public RedisTestContext(RedisServer server) {
		this.server = server;
		this.client = client(server);
	}

	@Override
	public String toString() {
		return server.toString();
	}

	private AbstractRedisClient client(RedisServer server) {
		if (server.isCluster()) {
			return RedisModulesClusterClient.create(server.getRedisURI());
		}
		return RedisModulesClient.create(server.getRedisURI());
	}

	public RedisServer getServer() {
		return server;
	}

	public AbstractRedisClient getClient() {
		return client;
	}

	public StatefulRedisModulesConnection<String, String> getConnection() {
		if (connection == null) {
			this.connection = connection();
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
		if (client instanceof RedisModulesClusterClient) {
			return ((RedisModulesClusterClient) client).connect();
		}
		return ((RedisModulesClient) client).connect();
	}

	private StatefulRedisPubSubConnection<String, String> pubSubConnection() {
		if (client instanceof RedisModulesClusterClient) {
			return ((RedisModulesClusterClient) client).connectPubSub();
		}
		return ((RedisModulesClient) client).connectPubSub();
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
		}
		if (connection != null) {
			connection.close();
		}
		client.shutdown();
		client.getResources().shutdown();
	}

	public RedisClient getRedisClient() {
		return (RedisClient) client;
	}

	public RedisClusterClient getRedisClusterClient() {
		return (RedisClusterClient) client;
	}

	public boolean isCluster() {
		return client instanceof RedisClusterClient;
	}

}

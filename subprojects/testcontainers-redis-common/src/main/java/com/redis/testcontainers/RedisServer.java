package com.redis.testcontainers;

public interface RedisServer {

	/**
	 * Returns URI of Redis server
	 *
	 * @return Redis URI.
	 */
	default String getRedisURI() {
		return "redis://" + getRedisHost() + ":" + getRedisPort();
	}

	String getRedisHost();

	int getRedisPort();

	/**
	 * 
	 * @return true if this is a Redis Cluster
	 */
	boolean isRedisCluster();

}

package com.redis.testcontainers;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

public interface MemcachedServer {

	/**
	 * Returns address of Memcached server
	 *
	 * @return Memcached server address.
	 */
	default List<InetSocketAddress> getMemcachedAddresses() {
		return Arrays.asList(new InetSocketAddress(getMemcachedHost(), getMemcachedPort()));
	}

	String getMemcachedHost();

	int getMemcachedPort();

}

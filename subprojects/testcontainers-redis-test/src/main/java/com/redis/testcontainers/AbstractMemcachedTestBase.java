package com.redis.testcontainers;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.testcontainers.lifecycle.Startable;

import net.spy.memcached.MemcachedClient;

@TestInstance(Lifecycle.PER_CLASS)
public abstract class AbstractMemcachedTestBase {

	private MemcachedServer server;
	private MemcachedClient client;

	protected abstract MemcachedServer getMemcachedServer();

	@BeforeAll
	public void setup() throws IOException {
		server = getMemcachedServer();
		if (server instanceof Startable) {
			((Startable) server).start();
		}
		client = new MemcachedClient(server.getMemcachedAddresses());
	}

	@AfterAll
	public void teardown() {
		if (client != null) {
			client.shutdown();
		}
		if (server instanceof Startable) {
			((Startable) server).stop();
		}
	}

	@BeforeEach
	void flushall() {
		client.flush();
	}

	@Test
	void testSet() {
		String key = "testkey";
		String value = "value";
		client.set(key, 30, value);
		Assertions.assertEquals(value, client.get(key));
	}

}

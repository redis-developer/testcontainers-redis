package com.redis.testcontainers.junit;

import java.util.List;

public interface RedisTestInstance {

	List<RedisTestContext> getContexts();

}

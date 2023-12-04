package com.redis.testcontainers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.redis.lettucemod.RedisModulesClient;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.lettucemod.api.sync.RedisModulesCommands;
import com.redis.lettucemod.search.Field;
import com.redis.lettucemod.search.IndexInfo;
import com.redis.lettucemod.util.RedisModulesUtils;

@SuppressWarnings("unchecked")
@Testcontainers
class RedisStackExampleTest {

    @Container
    private static RedisStackContainer container = new RedisStackContainer(
            RedisStackContainer.DEFAULT_IMAGE_NAME.withTag(RedisStackContainer.DEFAULT_TAG));

    @Test
    void testSomethingUsingLettuceMod() {
        // Retrieve the Redis URI from the container
        String redisURI = container.getRedisURI();
        RedisModulesClient client = RedisModulesClient.create(redisURI);
        try (StatefulRedisModulesConnection<String, String> connection = client.connect()) {
            RedisModulesCommands<String, String> commands = connection.sync();
            commands.ftCreate("myIndex", Field.tag("myField").build());
            IndexInfo indexInfo = RedisModulesUtils.indexInfo(commands.ftInfo("myIndex"));
            Assertions.assertEquals(1, indexInfo.getFields().size());
            Assertions.assertEquals("myField", indexInfo.getFields().get(0).getName());
        }
    }

}

package com.redislabs.testcontainers;

import com.redislabs.mesclun.RedisModulesClient;
import com.redislabs.mesclun.api.StatefulRedisModulesConnection;
import com.redislabs.mesclun.api.sync.RedisGearsCommands;
import com.redislabs.mesclun.cluster.RedisModulesClusterClient;
import com.redislabs.mesclun.cluster.api.StatefulRedisModulesClusterConnection;
import com.redislabs.mesclun.gears.RedisGearsUtils;
import com.redislabs.mesclun.gears.output.ExecutionResults;
import com.redislabs.mesclun.search.Field;
import com.redislabs.mesclun.search.SearchResults;
import com.redislabs.testcontainers.support.enterprise.rest.Database;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class TestRedisEnterpriseContainer {

    @Test
    void singleShard() {
        RedisEnterpriseContainer container = new RedisEnterpriseContainer();
        try {
            container.start();
            RedisClient client = RedisClient.create(container.getRedisURI());
            try (StatefulRedisConnection<String, String> connection = client.connect()) {
                Assertions.assertEquals("PONG", connection.sync().ping());
            } finally {
                client.shutdown();
                client.getResources().shutdown();
            }
        } finally {
            container.stop();
        }
    }

    @Test
    void cluster() {
        RedisEnterpriseContainer container = new RedisEnterpriseContainer();
        container.withShardCount(3);
        container.withOSSCluster();
        try {
            container.start();
            RedisClusterClient client = RedisClusterClient.create(container.getRedisURI());
            try (StatefulRedisClusterConnection<String, String> connection = client.connect()) {
                Assertions.assertEquals("PONG", connection.sync().ping());
            } finally {
                client.shutdown();
                client.getResources().shutdown();
            }
        } finally {
            container.stop();
        }
    }

    @Test
    void searchCluster() {
        RedisEnterpriseContainer container = new RedisEnterpriseContainer();
        container.withShardCount(3);
        container.withOSSCluster();
        container.withModules(Database.Module.SEARCH);
        try {
            container.start();
            RedisModulesClusterClient client = RedisModulesClusterClient.create(container.getRedisURI());
            try (StatefulRedisModulesClusterConnection<String, String> connection = client.connect()) {
                connection.sync().create("test", Field.text("name").build(), Field.tag("id").build());
                for (int index = 0; index < 10; index++) {
                    Map<String, String> hash = new HashMap<>();
                    hash.put("name", "name " + index);
                    hash.put("id", String.valueOf(index + 1));
                    connection.sync().hset("hash:" + index, hash);
                }
                SearchResults<String, String> results = connection.sync().search("test", "*");
                Assertions.assertEquals(10, results.getCount());
            } finally {
                client.shutdown();
                client.getResources().shutdown();
            }
        } finally {
            container.stop();
        }
    }

    @Test
    void gears() {
        RedisEnterpriseContainer container = new RedisEnterpriseContainer();
        container.withShardCount(3);
        container.withModules(Database.Module.GEARS);
        try {
            container.start();
            RedisModulesClient client = RedisModulesClient.create(container.getRedisURI());
            try (StatefulRedisModulesConnection<String, String> connection = client.connect()) {
                connection.sync().set("foo", "bar");
                ExecutionResults results = pyExecute(connection.sync(), "sleep.py");
                Assertions.assertEquals("1", results.getResults().get(0));
            }
        } finally {
            container.stop();
        }
    }

    @Test
    void gearsCluster() {
        RedisEnterpriseContainer container = new RedisEnterpriseContainer();
        container.withShardCount(3);
        container.withOSSCluster();
        container.withModules(Database.Module.GEARS);
        try {
            container.start();
            RedisModulesClusterClient client = RedisModulesClusterClient.create(container.getRedisURI());
            try (StatefulRedisModulesClusterConnection<String, String> connection = client.connect()) {
                connection.sync().set("foo", "bar");
                ExecutionResults results = pyExecute(connection.sync(), "sleep.py");
                Assertions.assertEquals("1", results.getResults().get(0));
            }
        } finally {
            container.stop();
        }
    }

    private ExecutionResults pyExecute(RedisGearsCommands<String, String> sync, String resourceName) {
        return sync.pyExecute(load(resourceName));
    }

    private String load(String resourceName) {
        return RedisGearsUtils.toString(getClass().getClassLoader().getResourceAsStream(resourceName));
    }

}

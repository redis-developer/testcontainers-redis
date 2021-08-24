package com.redis.testcontainers.support.enterprise.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.Arrays;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Database {

    private String name;
    private Boolean replication;
    private Boolean sharding;
    @Builder.Default
    @JsonProperty("memory_size")
    private long memory = DatabaseBuilder.DEFAULT_MEMORY;
    private Integer port;
    private String type;
    @JsonProperty("oss_cluster")
    private Boolean ossCluster;
    @JsonProperty("proxy_policy")
    private ProxyPolicy proxyPolicy;
    @JsonProperty("oss_cluster_api_preferred_ip_type")
    private IPType ossClusterAPIPreferredIPType;
    @JsonProperty("shard_key_regex")
    private List<Regex> shardKeyRegex;
    @JsonProperty("shards_count")
    private Integer shardCount;
    @JsonProperty("shards_placement")
    private ShardPlacement shardPlacement;
    @Singular
    @JsonProperty("module_list")
    private List<ModuleConfig> moduleConfigs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @EqualsAndHashCode()
    public static class ModuleConfig {

        @JsonProperty("module_name")
        private String name;
        @JsonProperty("module_id")
        @EqualsAndHashCode.Exclude
        private String id;
        @Builder.Default
        @JsonProperty("module_args")
        @EqualsAndHashCode.Exclude
        private String args = "";

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Regex {

        private String regex;

        public static Regex of(String regex) {
            return new Regex(regex);
        }

    }

    public enum ShardPlacement {
        @JsonProperty("dense")
        DENSE,
        @JsonProperty("sparse")
        SPARSE
    }

    public enum ProxyPolicy {
        @JsonProperty("single")
        SINGLE,
        @JsonProperty("all-master-shards")
        ALL_MASTER_SHARDS,
        @JsonProperty("all-nodes")
        ALL_NODES
    }

    public enum IPType {
        @JsonProperty("internal")
        INTERNAL,
        @JsonProperty("external")
        EXTERNAL
    }

    public enum Module {

        BLOOM("bf"), GEARS("rg"), GRAPH("graph"), JSON("ReJSON"), SEARCH("search"), TIMESERIES("timeseries");

        @Getter
        private String name;

        Module(String name) {
            this.name = name;
        }
    }

    private static DatabaseBuilder builder() {
        return new DatabaseBuilder();
    }

    public static DatabaseBuilder name(String name) {
        return new DatabaseBuilder(name);
    }

    public static class DatabaseBuilder {

        public DatabaseBuilder modules(Module... modules) {
            for (Module module : modules) {
                moduleConfig(ModuleConfig.builder().name(module.getName()).build());
            }
            return this;
        }

        public static final long DEFAULT_MEMORY = 520428800;
        public static final int DEFAULT_CLUSTER_SHARD_COUNT = 3;
        public static final List<Database.Regex> DEFAULT_SHARD_KEY_REGEX = Arrays.asList(Database.Regex.of(".*\\{(?<tag>.*)\\}.*"), Database.Regex.of("(?<tag>.*)"));

        private DatabaseBuilder() {
            super();
        }

        public DatabaseBuilder(String name) {
            this.name = name;
        }

        public DatabaseBuilder shardCount(int shardCount) {
            this.shardCount = shardCount;
            if (shardCount > 1) {
                this.sharding = true;
                this.shardKeyRegex = DEFAULT_SHARD_KEY_REGEX;
            }
            return this;
        }

        public DatabaseBuilder ossCluster(boolean ossCluster) {
            this.ossCluster = ossCluster;
            if (ossCluster) {
                this.proxyPolicy = ProxyPolicy.ALL_MASTER_SHARDS;
                this.ossClusterAPIPreferredIPType = IPType.EXTERNAL;
                if (shardCount == null || shardCount < 2) {
                    shardCount(DEFAULT_CLUSTER_SHARD_COUNT);
                }
            }
            return this;

        }
    }

}

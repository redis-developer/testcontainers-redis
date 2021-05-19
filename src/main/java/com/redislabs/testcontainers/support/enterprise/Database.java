package com.redislabs.testcontainers.support.enterprise;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Database {

    public static final String DEFAULT_TYPE = "redis";
    private static final long DEFAULT_MEMORY = 52428800;
    private static final int DEFAULT_PORT = 12000;

    private String name;
    private Boolean replication;
    private Boolean sharding;
    @Builder.Default
    @JsonProperty("memory_size")
    private long memory = DEFAULT_MEMORY;
    @Builder.Default
    private int port = DEFAULT_PORT;
    @Builder.Default
    private String type = DEFAULT_TYPE;
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
    @JsonProperty("module_list")
    private List<Module> moduleList;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Module {

        @JsonProperty("module_name")
        private String name;
        @JsonProperty("module_id")
        private String id;
        @Builder.Default
        @JsonProperty("module_args")
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
}

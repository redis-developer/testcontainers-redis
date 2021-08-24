package com.redis.testcontainers.support.enterprise.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModuleInstallResponse {

    @JsonProperty("action_uid")
    private String actionUID;
    @JsonProperty("uid")
    private String uid;

}

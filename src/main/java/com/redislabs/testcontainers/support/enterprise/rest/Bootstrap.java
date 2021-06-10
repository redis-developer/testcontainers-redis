package com.redislabs.testcontainers.support.enterprise.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Bootstrap {

    @JsonProperty("bootstrap_status")
    Status status;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Status {
        private String state;
    }

}

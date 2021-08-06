package com.redislabs.testcontainers.support.enterprise.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Module {

    @JsonProperty("module_name")
    private String name;
    @JsonProperty("uid")
    private String id;

}

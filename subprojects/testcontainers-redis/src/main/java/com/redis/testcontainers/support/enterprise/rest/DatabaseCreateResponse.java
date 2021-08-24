package com.redis.testcontainers.support.enterprise.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseCreateResponse {

    private String name;
    private long uid;

}

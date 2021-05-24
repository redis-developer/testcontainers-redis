package com.redislabs.testcontainers.support.enterprise.rest;

import lombok.Data;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;

@Data
public class CommandResponse {

    private JsonNode response;

}

package com.redis.testcontainers.support.enterprise.rest;

import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;

public class CommandResponse {

	private JsonNode response;

	public JsonNode getResponse() {
		return response;
	}

	public void setResponse(JsonNode response) {
		this.response = response;
	}

}

package com.redis.enterprise.rest;

import com.fasterxml.jackson.databind.JsonNode;

public class CommandResponse {

	private JsonNode response;

	public JsonNode getResponse() {
		return response;
	}

	public void setResponse(JsonNode response) {
		this.response = response;
	}

}

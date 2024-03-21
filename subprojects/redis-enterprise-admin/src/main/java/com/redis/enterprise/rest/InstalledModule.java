package com.redis.enterprise.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InstalledModule {

	private String name;
	private String id;

	public String getName() {
		return name;
	}

	@JsonProperty("module_name")
	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	@JsonProperty("uid")
	public void setId(String id) {
		this.id = id;
	}

}

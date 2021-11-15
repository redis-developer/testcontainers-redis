package com.redis.testcontainers.support.enterprise.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ModuleInstallResponse {

	private String actionUID;
	private String uid;

	public String getActionUID() {
		return actionUID;
	}

	@JsonProperty("action_uid")
	public void setActionUID(String actionUID) {
		this.actionUID = actionUID;
	}

	public String getUid() {
		return uid;
	}

	@JsonProperty("uid")
	public void setUid(String uid) {
		this.uid = uid;
	}

}

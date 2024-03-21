package com.redis.enterprise.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ModuleInstallResponse {

	private String actionUid;
	private String uid;

	public String getActionUid() {
		return actionUid;
	}

	@JsonProperty("action_uid")
	public void setActionUid(String actionUid) {
		this.actionUid = actionUid;
	}

	public String getUid() {
		return uid;
	}

	@JsonProperty("uid")
	public void setUid(String uid) {
		this.uid = uid;
	}

}

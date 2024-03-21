package com.redis.enterprise.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Action {

	private String actionUID;
	private String moduleUID;
	private String name;
	private String progress;
	private String status;
	private String taskID;

	public String getActionUID() {
		return actionUID;
	}

	@JsonProperty("action_uid")
	public void setActionUID(String actionUID) {
		this.actionUID = actionUID;
	}

	public String getModuleUID() {
		return moduleUID;
	}

	@JsonProperty("module_uid")
	public void setModuleUID(String moduleUID) {
		this.moduleUID = moduleUID;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getProgress() {
		return progress;
	}

	public void setProgress(String progress) {
		this.progress = progress;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getTaskID() {
		return taskID;
	}

	@JsonProperty("task_id")
	public void setTaskID(String taskID) {
		this.taskID = taskID;
	}

}

package com.redis.testcontainers.support.enterprise.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Bootstrap {

	private Status status;

	public Status getStatus() {
		return status;
	}

	@JsonProperty("bootstrap_status")
	public void setStatus(Status status) {
		this.status = status;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Status {

		private String state;

		public String getState() {
			return state;
		}

		public void setState(String state) {
			this.state = state;
		}

	}

}

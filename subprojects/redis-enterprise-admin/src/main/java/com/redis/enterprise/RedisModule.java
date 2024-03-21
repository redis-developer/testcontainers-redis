package com.redis.enterprise;

public enum RedisModule {

	BLOOM("bf"), GRAPH("graph"), JSON("ReJSON"), SEARCH("search"), TIMESERIES("timeseries");

	private final String moduleName;

	RedisModule(String moduleName) {
		this.moduleName = moduleName;
	}

	public String getModuleName() {
		return moduleName;
	}

}
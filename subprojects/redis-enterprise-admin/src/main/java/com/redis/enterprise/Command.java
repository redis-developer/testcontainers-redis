package com.redis.enterprise;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Command {

	private String name;
	private List<String> args = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String command) {
		this.name = command;
	}

	public List<String> getArgs() {
		return args;
	}

	public void setArgs(List<String> args) {
		this.args = args;
	}

	public static Builder name(String name) {
		return new Builder(name);
	}

	public static class Builder {

		private final String name;
		private List<String> args = new ArrayList<>();

		public Builder(String name) {
			this.name = name;
		}

		public Builder args(String... args) {
			this.args = new ArrayList<>(Arrays.asList(args));
			return this;
		}

		public Command build() {
			Command command = new Command();
			command.setName(name);
			command.setArgs(args);
			return command;
		}

	}

}

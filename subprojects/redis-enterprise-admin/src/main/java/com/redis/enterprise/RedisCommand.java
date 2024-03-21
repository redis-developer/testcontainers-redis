package com.redis.enterprise;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RedisCommand {

	private String command;
	private List<String> args = new ArrayList<>();

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
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

		private final String command;
		private List<String> args = new ArrayList<>();

		public Builder(String name) {
			this.command = name;
		}

		public Builder args(String... args) {
			this.args = new ArrayList<>(Arrays.asList(args));
			return this;
		}

		public RedisCommand build() {
			RedisCommand cmd = new RedisCommand();
			cmd.setCommand(this.command);
			cmd.setArgs(args);
			return cmd;
		}

	}

}

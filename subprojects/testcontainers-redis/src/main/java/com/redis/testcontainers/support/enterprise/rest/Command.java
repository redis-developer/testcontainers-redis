package com.redis.testcontainers.support.enterprise.rest;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Command {

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

	public static Command of(String commandString) {
		Command command = new Command();
		command.setCommand(commandString);
		return command;
	}

}

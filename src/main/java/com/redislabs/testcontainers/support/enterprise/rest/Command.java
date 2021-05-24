package com.redislabs.testcontainers.support.enterprise.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.testcontainers.shaded.com.google.common.base.Preconditions;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Command {

    private String command;
    private List<String> args;

    public static CommandBuilder command(String command) {
        return new CommandBuilder(command);
    }

    @Setter
    @Accessors(fluent = true)
    public static class CommandBuilder {

        private final String command;
        private List<String> args;

        public CommandBuilder(String command) {
            Preconditions.checkNotNull(command, "Command must not be null");
            this.command = command;
        }

        public Command build() {
            return new Command(command, args);
        }

    }

}

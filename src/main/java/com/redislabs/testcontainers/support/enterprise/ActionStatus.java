package com.redislabs.testcontainers.support.enterprise;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionStatus {

    @JsonProperty("action_uid")
    private String actionUID;
    @JsonProperty("module_uid")
    private String moduleUID;
    private String name;
    private String progress;
    private String status;
    @JsonProperty("task_id")
    private String taskID;

}

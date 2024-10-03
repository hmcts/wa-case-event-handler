package uk.gov.hmcts.reform.wacaseeventhandler.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;


@Schema(
    name = "TaskOperation",
    description = "Allows specifying certain operations on a task"
)
@EqualsAndHashCode
@ToString
public class TaskOperation {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("name")
    private final TaskOperationName name;

    @JsonProperty("run_id")
    private final String runId;

    @JsonProperty("max_time_limit")
    private final long maxTimeLimit;

    @JsonCreator
    public TaskOperation(@JsonProperty("name") TaskOperationName name,@JsonProperty("run_id") String runId,
                         @JsonProperty("max_time_limit") long maxTimeLimit) {
        this.name = name;
        this.runId = runId;
        this.maxTimeLimit = maxTimeLimit;
    }

    public TaskOperationName getName() {
        return name;
    }

    public String getRunId() {
        return runId;
    }

    public long getMaxTimeLimit() {
        return maxTimeLimit;
    }
}

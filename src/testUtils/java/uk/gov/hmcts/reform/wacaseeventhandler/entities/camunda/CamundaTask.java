package uk.gov.hmcts.reform.wacaseeventhandler.entities.camunda;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.ZonedDateTime;

import static uk.gov.hmcts.reform.wacaseeventhandler.entities.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMAT;

@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class CamundaTask {

    private final String id;
    private final String name;
    private final String processInstanceId;
    private final String assignee;
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    @JsonFormat(pattern = CAMUNDA_DATA_TIME_FORMAT)
    private final ZonedDateTime created;
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    @JsonFormat(pattern = CAMUNDA_DATA_TIME_FORMAT)
    private final ZonedDateTime due;
    private final String description;
    private final String owner;
    private final String formKey;

    public CamundaTask(String id,
                       String name,
                       String processInstanceId) {

        this(id, name, processInstanceId, null, null, null, null, null, null);
    }

    public CamundaTask(@JsonProperty("id") String id,
                       @JsonProperty("name") String name,
                       @JsonProperty("processInstanceId") String processInstanceId,
                       @JsonProperty("assignee") String assignee,
                       @JsonProperty("created") ZonedDateTime created,
                       @JsonProperty("due") ZonedDateTime due,
                       @JsonProperty("description") String description,
                       @JsonProperty("owner") String owner,
                       @JsonProperty("formKey") String formKey
    ) {
        this.id = id;
        this.name = name;
        this.assignee = assignee;
        this.created = created;
        this.due = due;
        this.description = description;
        this.owner = owner;
        this.formKey = formKey;
        this.processInstanceId = processInstanceId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAssignee() {
        return assignee;
    }

    public ZonedDateTime getCreated() {
        return created;
    }

    public ZonedDateTime getDue() {
        return due;
    }

    public String getDescription() {
        return description;
    }

    public String getOwner() {
        return owner;
    }

    public String getFormKey() {
        return formKey;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

}

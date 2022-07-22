package uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.io.Serializable;
import java.util.List;

@Builder
public class JobResponse  implements Serializable {

    private static final long serialVersionUID = 3213665988741833671L;

    private final String jobName;
    private final Integer numberOfMessages;
    private final List<String> messageIds;

    public JobResponse(@JsonProperty("jobName") String jobName,
                       @JsonProperty("numberOfMessages") Integer numberOfMessages,
                       @JsonProperty("messageIds") List<String> messageIds) {
        this.jobName = jobName;
        this.numberOfMessages = numberOfMessages;
        this.messageIds = messageIds;
    }

    public String getJobName() {
        return jobName;
    }

    public Integer getNumberOfMessages() {
        return numberOfMessages;
    }

    public List<String> getMessageIds() {
        return messageIds;
    }
}

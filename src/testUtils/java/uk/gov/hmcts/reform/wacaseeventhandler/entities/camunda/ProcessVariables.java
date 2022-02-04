package uk.gov.hmcts.reform.wacaseeventhandler.entities.camunda;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class ProcessVariables {
    private final String id;
    private final String definitionId;
    private final String businessKey;
    private final String caseInstanceId;
    private final Boolean suspended;
    private final Boolean ended;
    private final String tenantId;

    @JsonCreator
    public ProcessVariables(String id,
                            String definitionId,
                            String businessKey,
                            String caseInstanceId,
                            Boolean suspended,
                            Boolean ended,
                            String tenantId) {
        this.id = id;
        this.definitionId = definitionId;
        this.businessKey = businessKey;
        this.caseInstanceId = caseInstanceId;
        this.suspended = suspended;
        this.ended = ended;
        this.tenantId = tenantId;
    }

    public String getId() {
        return id;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public Boolean getSuspended() {
        return suspended;
    }

    public Boolean getEnded() {
        return ended;
    }

    public String getTenantId() {
        return tenantId;
    }

}

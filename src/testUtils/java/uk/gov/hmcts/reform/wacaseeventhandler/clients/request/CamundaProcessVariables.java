package uk.gov.hmcts.reform.wacaseeventhandler.clients.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnBooleanValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnStringValue;

@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode
@ToString
public class CamundaProcessVariables {

    Map<String, DmnValue<?>> processVariablesMap;

    @JsonCreator
    public CamundaProcessVariables(Map<String, DmnValue<?>> processVariablesMap) {
        this.processVariablesMap = processVariablesMap;
    }

    public Map<String, DmnValue<?>> getProcessVariablesMap() {
        return processVariablesMap;
    }

    public static class ProcessVariablesBuilder implements Builder<CamundaProcessVariables> {

        Map<String, DmnValue<?>> processVariablesMap = new ConcurrentHashMap<>();

        public static ProcessVariablesBuilder processVariables() {
            return new ProcessVariablesBuilder();
        }

        public ProcessVariablesBuilder withProcessVariable(String key, String value) {
            processVariablesMap.put(key, dmnStringValue(value));
            return this;
        }

        public ProcessVariablesBuilder withProcessVariableBoolean(String key, boolean value) {
            processVariablesMap.put(key, dmnBooleanValue(value));
            return this;
        }

        @Override
        public CamundaProcessVariables build() {
            return new CamundaProcessVariables(processVariablesMap);
        }
    }
}


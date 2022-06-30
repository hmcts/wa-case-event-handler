package uk.gov.hmcts.reform.wacaseeventhandler.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
    @JsonSubTypes.Type(value = MarkTaskToReconfigureTaskFilter.class, name = "MarkTaskToReconfigureTaskFilter"),
    @JsonSubTypes.Type(value = ExecuteReconfigureTaskFilter.class, name = "ExecuteReconfigureTaskFilter") }
)
public interface TaskFilter<T> {

    String getKey();

    TaskFilterOperator getOperator();

    T getValues();
}

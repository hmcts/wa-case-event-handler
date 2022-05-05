package uk.gov.hmcts.reform.wacaseeventhandler.entities.enums;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum ActorIdType {
    IDAM, CASEPARTY, @JsonEnumDefaultValue UNKNOWN
}

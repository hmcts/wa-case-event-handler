package uk.gov.hmcts.reform.wacaseeventhandler.entities.enums;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum RoleCategory {
    JUDICIAL, LEGAL_OPERATIONS, @JsonEnumDefaultValue UNKNOWN
}

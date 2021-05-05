package uk.gov.hmcts.reform.wacaseeventhandler.domain.ia;

public enum CaseEventFieldsDefinition {

    APPEAL_TYPE("appealType"),
    LAST_MODIFIED_DIRECTION("lastModifiedDirection"),
    DIRECTION_DUE_DATE("directionDueDate");

    private final String value;

    CaseEventFieldsDefinition(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}

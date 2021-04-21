package uk.gov.hmcts.reform.wacaseeventhandler.domain.ia;

public enum CaseEventFieldsDefinition {

    APPEAL_TYPE("appealType");

    private final String value;

    CaseEventFieldsDefinition(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}

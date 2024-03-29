package uk.gov.hmcts.reform.wacaseeventhandler.domain.model;

public enum TaskOperationName {
    MARK_TO_RECONFIGURE("mark_to_reconfigure"),
    EXECUTE_RECONFIGURE("execute_reconfigure");

    private final String value;

    TaskOperationName(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}

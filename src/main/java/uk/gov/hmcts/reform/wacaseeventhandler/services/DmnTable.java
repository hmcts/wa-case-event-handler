package uk.gov.hmcts.reform.wacaseeventhandler.services;

public enum DmnTable {

    TASK_CANCELLATION("wa-task-cancellation"),
    TASK_INITIATION("wa-task-initiation");

    private final String tableName;

    DmnTable(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getTableKey(String jurisdictionId, String caseTypeId) {
        return tableName + "-" + jurisdictionId + "-" + caseTypeId;
    }
}

package uk.gov.hmcts.reform.wacaseeventhandler.services;

public enum HandlerConstants {

    TASK_CANCELLATION("wa-task-cancellation", "cancelTasks"),
    TASK_INITIATION("wa-task-initiation", "createTaskMessage"),
    TASK_WARN("wa-task-initiation", "warnProcess");

    private final String tableName;
    private final String messageName;

    HandlerConstants(String tableName, String messageName) {
        this.tableName = tableName;
        this.messageName = messageName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getMessageName() {
        return messageName;
    }

    public String getTableKey(String jurisdictionId, String caseTypeId) {
        return tableName + "-" + jurisdictionId + "-" + caseTypeId;
    }

    public String getTenantId(String tenantId) {
        return tenantId;
    }

}

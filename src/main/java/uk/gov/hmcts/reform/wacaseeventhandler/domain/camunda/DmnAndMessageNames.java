package uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda;

import java.util.Locale;

import static java.lang.String.format;

public enum DmnAndMessageNames {

    TASK_CANCELLATION("wa-task-cancellation", "cancelTasks"),
    TASK_INITIATION("wa-task-initiation", "createTaskMessage"),
    TASK_WARN("wa-task-cancellation", "warnProcess");

    private final String tableName;
    private final String messageName;

    DmnAndMessageNames(String tableName, String messageName) {
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
        return format(
            "%s-%s-%s",
            tableName,
            jurisdictionId.toLowerCase(Locale.ENGLISH),
            caseTypeId.toLowerCase(Locale.ENGLISH)
        );
    }

}

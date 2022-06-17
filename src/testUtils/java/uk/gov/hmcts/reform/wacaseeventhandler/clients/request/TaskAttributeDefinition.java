package uk.gov.hmcts.reform.wacaseeventhandler.clients.request;

public enum TaskAttributeDefinition {
    TASK_ASSIGNEE("task_assignee"),
    TASK_ASSIGNMENT_EXPIRY("task_assignment_expiry"),
    TASK_AUTO_ASSIGNED("task_auto_assigned"),
    TASK_BUSINESS_CONTEXT("task_business_context"),
    TASK_CASE_ID("task_case_id"),
    TASK_CASE_NAME("task_case_name"),
    TASK_CASE_TYPE_ID("task_case_type_id"),
    TASK_CASE_CATEGORY("task_case_category"),
    TASK_CREATED("task_created"),
    TASK_DESCRIPTION("task_description"),
    TASK_DUE_DATE("task_due_date"),
    TASK_EXECUTION_TYPE_NAME("task_execution_type_name"),
    TASK_HAS_WARNINGS("task_has_warnings"),
    TASK_JURISDICTION("task_jurisdiction"),
    TASK_LOCATION("task_location"),
    TASK_LOCATION_NAME("task_location_name"),
    TASK_MAJOR_PRIORITY("task_major_priority"),
    TASK_MINOR_PRIORITY("task_minor_priority"),
    TASK_NAME("task_name"),
    TASK_NOTES("task_notes"),
    TASK_WARNINGS("task_warnings"),
    TASK_REGION("task_region"),
    TASK_REGION_NAME("task_region_name"),
    TASK_ROLE_CATEGORY("task_role_category"),
    TASK_ROLES("task_roles"),
    TASK_SECURITY_CLASSIFICATION("task_security_classification"),
    TASK_STATE("task_state"),
    TASK_SYSTEM("task_system"),
    TASK_TERMINATION_REASON("task_termination_reason"),
    TASK_TITLE("task_title"),
    TASK_TYPE("task_type"),
    TASK_WORK_TYPE("task_work_type");

    private final String value;

    TaskAttributeDefinition(String value) {
        this.value = value;

    }

    public String value() {
        return value;
    }

}

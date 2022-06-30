package uk.gov.hmcts.reform.wacaseeventhandler.config.features;

public enum FeatureFlag {


    //Logs
    AZURE_AMQP_LOGS("logs-azure-amqp"),
    AZURE_MESSAGING_SERVICE_BUS_LOGS("logs-azure-messaging-service-bus"),
    AZURE_SERVICE_BUS_LOGS("logs-azure-service-bus"),

    //Features
    TASK_INITIATION_FEATURE("wa-task-initiation-feature"),
    DLQ_DB_INSERT("wa_dlq_database_insert"),
    DLQ_DB_PROCESS("wa_dlq_database_process"),

    //The following keys are used for testing purposes only.
    TEST_KEY("tester"),
    NON_EXISTENT_KEY("non-existent");

    private final String key;

    FeatureFlag(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}

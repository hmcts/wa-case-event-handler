package uk.gov.hmcts.reform.wacaseeventhandler;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

public final class TestUtility {

    private TestUtility() {
        //Utility classes should not have a public or default constructor.
    }

    public static String asJsonString(Object object) {
        try {
            return JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build()
                .writeValueAsString(object);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getExpectedRequestForUnconfiguredTasks() {
        return "{\n"
               + "  \"orQueries\": [\n"
               + "    {\n"
               + "      \"taskVariables\": [\n"
               + "        {\n"
               + "          \"name\": \"taskState\",\n"
               + "          \"operator\": \"eq\",\n"
               + "          \"value\": \"unconfigured\"\n"
               + "        }\n"
               + "      ]\n"
               + "    }\n"
               + "  ],\n"
               + "  \"taskDefinitionKey\": \"processTask\",\n"
               + "  \"processDefinitionKey\": \"wa-task-initiation-ia-asylum\"\n"
               + "}\n";
    }

    public static String getExpectedRequestForHistoricTasksPendingTermination() {
        return "{\n"
               + "  \"taskVariables\": [\n"
               + "    {\n"
               + "      \"name\": \"cftTaskState\",\n"
               + "      \"operator\": \"eq\",\n"
               + "      \"value\": \"pendingTermination\"\n"
               + "    }\n"
               + "  ],\n"
               + "  \"processDefinitionKey\": \"wa-task-initiation-ia-asylum\"\n"
               + "}\n";
    }
}

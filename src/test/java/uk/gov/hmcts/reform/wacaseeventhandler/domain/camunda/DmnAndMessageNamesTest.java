package uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnAndMessageNames.TASK_CANCELLATION;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnAndMessageNames.TASK_INITIATION;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnAndMessageNames.TASK_WARN;

class DmnAndMessageNamesTest {

    @Test
    void should_format_table_key_name() {
        String tableWarningName = TASK_WARN.getTableKey("ia", "asylum");
        assertThat(tableWarningName, is("wa-task-cancellation-ia-asylum"));

        String tableCancellationName = TASK_CANCELLATION.getTableKey("ia", "asylum");
        assertThat(tableCancellationName, is("wa-task-cancellation-ia-asylum"));

        String tableInitiationName = TASK_INITIATION.getTableKey("ia", "asylum");
        assertThat(tableInitiationName, is("wa-task-initiation-ia-asylum"));
    }

    @Test
    void should_convert_to_lower_case_table_key_name() {
        String tableWarningName = TASK_WARN.getTableKey("IA", "Asylum");
        assertThat(tableWarningName, is("wa-task-cancellation-ia-asylum"));

        String tableCancellationName = TASK_CANCELLATION.getTableKey("IA", "Asylum");
        assertThat(tableCancellationName, is("wa-task-cancellation-ia-asylum"));

        String tableInitiationName = TASK_INITIATION.getTableKey("Ia", "Asylum");
        assertThat(tableInitiationName, is("wa-task-initiation-ia-asylum"));
    }

}

package uk.gov.hmcts.reform.wacaseeventhandler.services.jobservices;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.wacaseeventhandler.config.job.CleanUpJobConfiguration;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
public class CleanUpMessageJobTest {

    @Mock
    private CaseEventMessageRepository caseEventMessageRepository;
    @Spy
    private CleanUpJobConfiguration cleanUpJobConfiguration;

    private CleanUpMessageJob cleanUpMessageJob;

    @BeforeEach
    void setUp() {

        cleanUpMessageJob = new CleanUpMessageJob(
            caseEventMessageRepository,
            cleanUpJobConfiguration
        );

    }

    @Test
    void should_be_able_to_run_clean_up_message_job() {
        assertTrue(cleanUpMessageJob.canRun(JobName.CLEAN_UP_MESSAGES));
    }

    @Test
    void should_not_be_able_to_run_clean_up_message_job_for_other_job_types() {
        assertFalse(cleanUpMessageJob.canRun(JobName.FIND_PROBLEM_MESSAGES));
    }

    @Test
    void should_clean_messages_when_environment_is_prod(CapturedOutput output) {

        ReflectionTestUtils.setField(cleanUpJobConfiguration, "environment", "PROD");
        ReflectionTestUtils.setField(cleanUpJobConfiguration, "deleteLimit", 5);
        ReflectionTestUtils.setField(cleanUpJobConfiguration, "startedDaysBefore", 90);
        ReflectionTestUtils.setField(cleanUpJobConfiguration, "stateForProd", List.of("PROCESSED"));
        ReflectionTestUtils.setField(cleanUpJobConfiguration, "stateForNonProd", List.of("PROCESSED"));

        doReturn(90).when(cleanUpJobConfiguration).getStartedDaysBefore();
        doReturn("PROD").when(cleanUpJobConfiguration).getEnvironment();
        doReturn(5).when(cleanUpJobConfiguration).getDeleteLimit();
        doReturn(List.of("PROCESSED")).when(cleanUpJobConfiguration).getStateForProd();

        List<String> response = cleanUpMessageJob.run();

        Assertions.assertTrue(response.equals(emptyList()));

        verify(caseEventMessageRepository, times(1))
            .removeOldMessages(
                anyInt(),
                any(),
                any()
            );

        String expectedLogMessage = "CleanUpJobConfiguration(environment=PROD, deleteLimit=5, startedDaysBefore=90, "
                                    + "stateForProd=[PROCESSED], stateForNonProd=[PROCESSED])";
        assertConsoleOutputHasMessages(output, expectedLogMessage);
    }

    @Test
    void should_clean_messages_when_environment_is_non_prod(CapturedOutput output) {

        ReflectionTestUtils.setField(cleanUpJobConfiguration, "environment", "aat");
        ReflectionTestUtils.setField(cleanUpJobConfiguration, "deleteLimit", 5);
        ReflectionTestUtils.setField(cleanUpJobConfiguration, "startedDaysBefore", 90);
        ReflectionTestUtils.setField(cleanUpJobConfiguration, "stateForProd", List.of("PROCESSED"));
        ReflectionTestUtils.setField(cleanUpJobConfiguration, "stateForNonProd", List.of("PROCESSED", "READY"));

        doReturn(90).when(cleanUpJobConfiguration).getStartedDaysBefore();
        doReturn("aat").when(cleanUpJobConfiguration).getEnvironment();
        doReturn(5).when(cleanUpJobConfiguration).getDeleteLimit();
        doReturn(List.of("PROCESSED")).when(cleanUpJobConfiguration).getStateForProd();
        doReturn(List.of("PROCESSED, READY")).when(cleanUpJobConfiguration).getStateForNonProd();

        List<String> response = cleanUpMessageJob.run();

        Assertions.assertTrue(response.equals(emptyList()));

        verify(caseEventMessageRepository, times(1))
            .removeOldMessages(
                anyInt(),
                any(),
                any()
            );

        String expectedLogMessage = "CleanUpJobConfiguration(environment=aat, deleteLimit=5, startedDaysBefore=90, "
                                    + "stateForProd=[PROCESSED], stateForNonProd=[PROCESSED, READY])";
        assertConsoleOutputHasMessages(output, expectedLogMessage);
    }

    private void assertConsoleOutputHasMessages(CapturedOutput output, String expectedLogMessage) {
        await().ignoreException(Exception.class)
            .pollInterval(100, MILLISECONDS)
            .atMost(5, SECONDS)
            .untilAsserted(() -> {
                Assertions.assertTrue(output.getOut().contains(expectedLogMessage));
                Assertions.assertTrue(output.getOut().contains("CLEAN_UP_MESSAGES job completed"));
            });
    }




}

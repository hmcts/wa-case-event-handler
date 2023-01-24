package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.apache.commons.collections4.IterableUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wacaseeventhandler.config.job.CleanUpJobConfiguration;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;
import uk.gov.hmcts.reform.wacaseeventhandler.services.jobservices.CleanUpMessageJob;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ActiveProfiles("integration")
@ExtendWith(SpringExtension.class)
@Import(CleanUpJobConfiguration.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql("/scripts/clean_up_message_data.sql")
@TestPropertySource(properties = {
    "environment=PROD",
    "job.clean-up.delete-limit=5",
    "job.clean-up.started-days-before=90",
    "job.clean-up.state-for-prod=PROCESSED",
    "job.clean-up.state-for-non-prod=PROCESSED",
})
public class CleanUpMessageJobTest {
    @Autowired
    private CaseEventMessageRepository caseEventMessageRepository;

    @Autowired
    private CleanUpJobConfiguration cleanUpJobConfiguration;

    private CleanUpMessageJob cleanUpMessageJob;

    @BeforeEach
    void setUp() {
        cleanUpMessageJob = new CleanUpMessageJob(
            caseEventMessageRepository,
            cleanUpJobConfiguration
        );
    }

    @AfterEach
    void tearDown() {
        caseEventMessageRepository.deleteAll();
    }

    @Test
    void should_delete_messages() {

        List<CaseEventMessageEntity> allRecords = IterableUtils.toList(caseEventMessageRepository.findAll());

        cleanUpMessageJob.run();

        List<CaseEventMessageEntity> allRecordsAfterCleanUpJob = IterableUtils.toList(
            caseEventMessageRepository.findAll());

        assertThat(allRecords.size()).isEqualTo(14);
        assertThat(allRecordsAfterCleanUpJob.size()).isEqualTo(0);

    }

    @Test
    void should_delete_1_record_when_delete_limit_is_1() {

        ReflectionTestUtils.setField(cleanUpJobConfiguration, "environment", "AAT");
        ReflectionTestUtils.setField(cleanUpJobConfiguration, "deleteLimit", 1);
        ReflectionTestUtils.setField(cleanUpJobConfiguration, "startedDaysBefore", 20);
        ReflectionTestUtils.setField(cleanUpJobConfiguration, "stateForProd", List.of("PROCESSED"));
        ReflectionTestUtils.setField(cleanUpJobConfiguration, "stateForNonProd",
            List.of("PROCESSED", "READY", "UNPROCESSABLE"));

        List<CaseEventMessageEntity> allRecords = IterableUtils.toList(caseEventMessageRepository.findAll());

        cleanUpMessageJob.run();

        List<CaseEventMessageEntity> allRecordsAfterCleanUpJob = IterableUtils.toList(
            caseEventMessageRepository.findAll());

        assertThat(allRecords.size()).isEqualTo(14);
        assertThat(allRecordsAfterCleanUpJob.size()).isEqualTo(13);

    }

    @Test
    void should_not_delete_any_record_when_state_is_invalid() {

        ReflectionTestUtils.setField(cleanUpJobConfiguration, "environment", "AAT");
        ReflectionTestUtils.setField(cleanUpJobConfiguration, "deleteLimit", 1);
        ReflectionTestUtils.setField(cleanUpJobConfiguration, "startedDaysBefore", 20);
        ReflectionTestUtils.setField(cleanUpJobConfiguration, "stateForNonProd", List.of("INVALID_STATE"));

        List<CaseEventMessageEntity> allRecords = IterableUtils.toList(caseEventMessageRepository.findAll());

        cleanUpMessageJob.run();

        List<CaseEventMessageEntity> allRecordsAfterCleanUpJob = IterableUtils.toList(
            caseEventMessageRepository.findAll());

        assertThat(allRecords.size()).isEqualTo(14);
        assertThat(allRecordsAfterCleanUpJob.size()).isEqualTo(14);

    }

    @Test
    void should_delete_all_records_when_env_is_non_prod_and_state_is_fully_matched() {

        ReflectionTestUtils.setField(cleanUpJobConfiguration, "environment", "local");
        ReflectionTestUtils.setField(cleanUpJobConfiguration, "deleteLimit", 14);
        ReflectionTestUtils.setField(cleanUpJobConfiguration, "startedDaysBefore", 20);
        ReflectionTestUtils.setField(cleanUpJobConfiguration, "stateForNonProd",
            List.of("PROCESSED", "READY", "UNPROCESSABLE"));

        List<CaseEventMessageEntity> allRecords = IterableUtils.toList(caseEventMessageRepository.findAll());

        cleanUpMessageJob.run();

        List<CaseEventMessageEntity> allRecordsAfterCleanUpJob = IterableUtils.toList(
            caseEventMessageRepository.findAll());

        assertThat(allRecords.size()).isEqualTo(14);
        assertThat(allRecordsAfterCleanUpJob.size()).isEqualTo(0);

    }


}

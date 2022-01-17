package uk.gov.hmcts.reform.wacaseeventhandler.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.DatabaseMessageConsumer;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.LaunchDarklyFeatureFlagProvider;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static uk.gov.hmcts.reform.wacaseeventhandler.config.features.FeatureFlag.DLQ_DB_INSERT;

@Configuration
@Slf4j
public class CcdMessageProcessorExecutor {

    @Value("${messageProcessing.pollIntervalSeconds}")
    private int pollInterval;

    @Autowired
    private DatabaseMessageConsumer databaseMessageConsumer;

    @Autowired
    private LaunchDarklyFeatureFlagProvider featureFlagProvider;

    @Bean
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public void createDatabaseMessageConsumer() {
        if (featureFlagProvider.getBooleanValue(DLQ_DB_INSERT)) {
            final ScheduledExecutorService scheduledExecutorService =
                    Executors.newScheduledThreadPool(1);

            scheduledExecutorService.scheduleAtFixedRate(databaseMessageConsumer, 5, pollInterval, TimeUnit.SECONDS);
        } else {
            log.info("Feature flag '{}' evaluated to false. Did not start message processor thread",
                    DLQ_DB_INSERT.getKey());
        }

    }

}

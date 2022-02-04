package uk.gov.hmcts.reform.wacaseeventhandler.config.executors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.DatabaseMessageConsumer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class CcdMessageProcessorExecutor {

    @Value("${scheduledExecutors.messageProcessing.pollIntervalSeconds}")
    private int pollInterval;

    @Autowired
    private DatabaseMessageConsumer databaseMessageConsumer;

    @Bean
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public void createDatabaseMessageConsumer() {
        final ScheduledExecutorService scheduledExecutorService =
                    Executors.newScheduledThreadPool(1);

        scheduledExecutorService.scheduleAtFixedRate(databaseMessageConsumer, 5, pollInterval, TimeUnit.SECONDS);
    }
}

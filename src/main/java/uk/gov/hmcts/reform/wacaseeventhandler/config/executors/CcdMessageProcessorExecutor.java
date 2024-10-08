package uk.gov.hmcts.reform.wacaseeventhandler.config.executors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.DatabaseMessageConsumer;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@Profile("!functional & !local")
public class CcdMessageProcessorExecutor {

    @Value("${scheduledExecutors.messageProcessing.pollIntervalMilliSeconds}")
    private int pollInterval;

    @Autowired
    private DatabaseMessageConsumer databaseMessageConsumer;

    @Autowired
    private ScheduledExecutorService databaseMessageExecutorService;

    @PostConstruct
    public void start() {
        log.info("Starting Database message executor");
        databaseMessageExecutorService.scheduleWithFixedDelay(databaseMessageConsumer,
            5000,
            pollInterval,
            TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void cleanup() {
        log.info("Shutting down Database message executor");
        databaseMessageExecutorService.shutdown();
        try {
            // Wait a while for existing tasks to terminate
            if (!databaseMessageExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
                databaseMessageExecutorService.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!databaseMessageExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            databaseMessageExecutorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
        log.info("Shut down Database message executor");
    }
}

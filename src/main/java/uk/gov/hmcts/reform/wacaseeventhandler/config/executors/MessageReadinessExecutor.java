package uk.gov.hmcts.reform.wacaseeventhandler.config.executors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.MessageReadinessConsumer;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;


@Component
@Slf4j
@ConditionalOnProperty("azure.servicebus.enableASB")
public class MessageReadinessExecutor {
    @Value("${scheduledExecutors.messageReadiness.pollIntervalSeconds}")
    private int pollInterval;

    @Autowired
    private MessageReadinessConsumer messageReadinessConsumer;

    @Autowired
    private ScheduledExecutorService messageReadinessExecutorService;


    @PostConstruct
    public void start() {
        log.info("Starting message readiness executor");
        messageReadinessExecutorService.scheduleAtFixedRate(messageReadinessConsumer,5000, pollInterval,
                                                            TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void cleanup() {
        log.info("Shutting down message readiness executor");
        messageReadinessExecutorService.shutdown();
        try {
            // Wait a while for existing tasks to terminate
            if (!messageReadinessExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
                messageReadinessExecutorService.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!messageReadinessExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            messageReadinessExecutorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
        log.info("Shut down message readiness executor");
    }
}

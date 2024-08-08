package uk.gov.hmcts.reform.wacaseeventhandler.config.executors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.CcdCaseEventsDeadLetterQueueConsumer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Slf4j
@Component
@ConditionalOnProperty("azure.servicebus.enableASB-DLQ")
@Profile("!functional & !local")
public class CcdCaseEventsDeadLetterQueueExecutor {

    @Value("${azure.servicebus.threads}")
    private int concurrentSessions;

    @Autowired
    private CcdCaseEventsDeadLetterQueueConsumer serviceBusTask;

    @Autowired
    private ExecutorService deadLetterQueueExecutorService;

    @PostConstruct
    public void start() {
        log.info("Starting DLQ executor");
        IntStream.range(0, concurrentSessions).forEach(
            task -> deadLetterQueueExecutorService.execute(serviceBusTask));
    }

    @PreDestroy
    public void cleanup() {
        log.info("Shutting down DLQ executor");
        serviceBusTask.stop();
        deadLetterQueueExecutorService.shutdown();
        try {
            // Wait a while for existing tasks to terminate
            if (!deadLetterQueueExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
                deadLetterQueueExecutorService.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!deadLetterQueueExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            deadLetterQueueExecutorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
        log.info("Shut down DLQ executor");
    }

}

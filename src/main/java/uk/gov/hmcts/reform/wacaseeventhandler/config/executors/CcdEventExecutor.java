package uk.gov.hmcts.reform.wacaseeventhandler.config.executors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.CcdEventConsumer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;


@Slf4j
@Component
@ConditionalOnProperty("azure.servicebus.enableASB")
@Profile("!functional & !local")
public class CcdEventExecutor {

    @Value("${azure.servicebus.threads}")
    private int concurrentSessions;

    @Autowired
    private CcdEventConsumer serviceBusTask;

    @Autowired
    private ExecutorService ccdEventExecutorService;

    @PostConstruct
    public void start() {
        log.info("Starting message executor");
        IntStream.range(0, concurrentSessions).forEach(
            task -> ccdEventExecutorService.execute(serviceBusTask));
    }

    @PreDestroy
    public void cleanup() {
        log.info("Shutting down message executor");
        serviceBusTask.stop();
        ccdEventExecutorService.shutdown();
        try {
            // Wait a while for existing tasks to terminate
            if (!ccdEventExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
                ccdEventExecutorService.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!ccdEventExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            ccdEventExecutorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
        log.info("Shut down message executor");
    }

}

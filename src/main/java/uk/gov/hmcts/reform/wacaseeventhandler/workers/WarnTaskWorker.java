package uk.gov.hmcts.reform.wacaseeventhandler.workers;

import java.util.logging.Logger;
import java.awt.Desktop;
import java.net.URI;

import org.camunda.bpm.client.ExternalTaskClient;

public class WarnTaskWorker {
    private static final Logger LOGGER = Logger.getLogger(WarnTaskWorker.class.getName());

    public static void main(String[] args) {
        ExternalTaskClient client = ExternalTaskClient.create()
            .baseUrl("http://camunda-bpm/engine-rest")
            .asyncResponseTimeout(10000) // long polling timeout
            .build();

        // subscribe to an external task topic as specified in the process
        client.subscribe("handleWarningProcess")
            .lockDuration(1000) // the default lock duration is 20 seconds, but you can override this
            .handler((externalTask, externalTaskService) -> {
                // Put your business logic here

                // Get a process variable
                var item = externalTask.getVariable("item");

                LOGGER.info("Charging credit card with an amount of '"  + "'€ for the item '" + item + "'...");

                // Complete the task
                externalTaskService.complete(externalTask);
            })
            .open();
    }
}

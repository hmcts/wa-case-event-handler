package uk.gov.hmcts.reform.wacaseeventhandler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
@EnableFeignClients(basePackages =
    {
        "uk.gov.hmcts.reform.ccd.client",
        "uk.gov.hmcts.reform.wacaseeventhandler.clients",
        "uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates",
        "uk.gov.hmcts.reform.wacaseeventhandler.entities.idam",
        "uk.gov.hmcts.reform.wacaseeventhandler.services"
    })
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

}

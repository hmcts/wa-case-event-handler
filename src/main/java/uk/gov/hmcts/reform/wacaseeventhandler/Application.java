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
        "uk.gov.hmcts.reform.ccd.client"
    })
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

}

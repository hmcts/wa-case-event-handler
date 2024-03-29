package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import uk.gov.hmcts.reform.wacaseeventhandler.config.SnakeCaseFeignConfiguration;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(
    name = "idam-api",
    url = "${idam.api.baseUrl}",
    configuration = SnakeCaseFeignConfiguration.class
)
public interface IdamServiceApi {

    @PostMapping(
        value = "/testing-support/accounts",
        consumes = APPLICATION_JSON_VALUE
    )
    void createTestUser(@RequestBody Map<String, ?> form);

    @DeleteMapping(
        value = "/testing-support/accounts/{username}",
        consumes = APPLICATION_JSON_VALUE
    )
    void deleteTestUser(@PathVariable("username") String username);


}

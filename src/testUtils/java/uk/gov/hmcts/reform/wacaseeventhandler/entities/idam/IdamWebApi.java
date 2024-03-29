package uk.gov.hmcts.reform.wacaseeventhandler.entities.idam;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.wacaseeventhandler.config.SnakeCaseFeignConfiguration;

import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(
    name = "idam-web-api",
    url = "${idam.url}",
    configuration = SnakeCaseFeignConfiguration.class
)
public interface IdamWebApi {
    @GetMapping(
        value = "/o/userinfo",
        produces = APPLICATION_JSON_VALUE,
        consumes = APPLICATION_JSON_VALUE
    )
    UserInfo userInfo(@RequestHeader(AUTHORIZATION) String userToken);

    @PostMapping(
        value = "/o/token",
        produces = APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    Token token(@RequestBody Map<String, ?> form);

}

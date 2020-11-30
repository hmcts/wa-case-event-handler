package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CaseEventHandlerController {

    @ApiOperation("Handles the CCD case event message")
    @PostMapping("/messages")
    public @ResponseBody String caseEventHandler(@RequestBody String ccdEventMessage) {
        return "Hello world!";
    }
}

package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.UnProcessableEntityException;

@RestController
public class CaseEventHandlerController {

    @ApiOperation("Handles the CCD case event message")
    @PostMapping("/messages")
    public void caseEventHandler(@RequestBody String ccdEventMessage) {
        if (!ccdEventMessage.equals("valid message")) {
            throw new UnProcessableEntityException("CCD event message is invalid");
        }
    }
}

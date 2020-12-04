package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.CcdEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CaseEventHandler;

import java.util.List;
import javax.validation.Valid;

@RestController
public class CaseEventHandlerController {

    @Autowired
    private List<CaseEventHandler> handlerServices;

    @ApiOperation("Handles the CCD case event message")
    @PostMapping(path = "/messages", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public void caseEventHandler(@Valid @RequestBody CcdEventMessage ccdEventMessage) {

        handlerServices.stream()
            .filter(CaseEventHandler::canHandle)
            .forEach(CaseEventHandler::handle);

    }

}

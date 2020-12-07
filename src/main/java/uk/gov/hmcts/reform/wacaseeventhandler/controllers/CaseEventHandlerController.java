package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CaseEventHandler;

import java.util.List;
import javax.validation.Valid;

import static org.springframework.http.ResponseEntity.noContent;

@RestController
public class CaseEventHandlerController {

    @Autowired
    private List<CaseEventHandler> handlerServices;

    @ApiOperation("Handles the CCD case event message")
    @PostMapping(path = "/messages", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Void> caseEventHandler(@Valid @RequestBody EventInformation eventInformation) {

        handlerServices.stream()
            .filter(handler -> handler.canHandle(eventInformation))
            .forEach(CaseEventHandler::handle);

        return noContent().build();

    }

}

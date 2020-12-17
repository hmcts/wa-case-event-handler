package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
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
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public ResponseEntity<Void> caseEventHandler(@Valid @RequestBody EventInformation eventInformation) {

        for (CaseEventHandler handler : handlerServices) {
            List<? extends EvaluateResponse> results = handler.evaluateDmn(eventInformation);
            if (!results.isEmpty()) {
                handler.handle(results, eventInformation);
            }
        }

        return noContent().build();

    }

}

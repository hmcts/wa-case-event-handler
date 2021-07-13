package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.CaseEventHandler;

import java.util.List;
import javax.validation.Valid;

import static org.springframework.http.ResponseEntity.noContent;

@RestController
public class CaseEventHandlerController {

    private final List<CaseEventHandler> handlerServices;

    public CaseEventHandlerController(List<CaseEventHandler> handlerServices) {
        this.handlerServices = handlerServices;
    }

    @ApiOperation("Handles the CCD case event message")
    @ApiResponses({
        @ApiResponse(
            code = 204,
            message = "Message processed successfully",
            response = Object.class)
    })
    @PostMapping(path = "/messages", consumes = {MediaType.APPLICATION_JSON_VALUE})
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    @ResponseStatus(HttpStatus.NO_CONTENT)
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

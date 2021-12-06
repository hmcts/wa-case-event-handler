package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.CaseEventHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.services.EventMessageReceiverService;

import javax.validation.Valid;
import java.util.List;

import static org.springframework.http.ResponseEntity.noContent;

@RestController
@Slf4j
public class CaseEventHandlerController {
    private final EventMessageReceiverService eventMessageReceiverService;
    private final List<CaseEventHandler> handlerServices;

    public CaseEventHandlerController(List<CaseEventHandler> handlerServices,
                                      EventMessageReceiverService eventMessageReceiverService) {
        this.handlerServices = handlerServices;
        this.eventMessageReceiverService = eventMessageReceiverService;
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
        log.info("incoming test message: {}", eventInformation);
        for (CaseEventHandler handler : handlerServices) {
            List<? extends EvaluateResponse> results = handler.evaluateDmn(eventInformation);
            if (!results.isEmpty()) {
                handler.handle(results, eventInformation);
            }
        }

        return noContent().build();

    }

    @ApiOperation("Handles the CCD case event message")
    @ApiResponses({
        @ApiResponse(
            code = 204,
            message = "Message processed successfully",
            response = Object.class)
    })
    @PostMapping(path = "/messages/{message_id}", consumes = {MediaType.APPLICATION_JSON_VALUE})
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> caseEventHandler(@Valid @RequestBody String message,
                                                 @PathVariable("message_id") final String messageId) {
        eventMessageReceiverService.handleAsbMessage(messageId, message);
        return noContent().build();
    }

    @ApiOperation("Gets the CCD case event messages by messageId")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Messages returned successfully",
            response = CaseEventMessageEntity.class,
            responseContainer = "List")
    })
    @GetMapping(value = "/messages/{message_id}")
    public CaseEventMessageEntity getMessagesByMessageId(@PathVariable("message_id") final String messageId) {
        // TODO: map to model and return domain object instead of the entity
        return eventMessageReceiverService.getMessage(messageId);
    }

    @ApiOperation("Gets the CCD case event messages")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Messages returned successfully",
            response = CaseEventMessageEntity.class,
            responseContainer = "List")
    })
    @GetMapping(value = "/messages")
    public List<CaseEventMessageEntity> getMessages() {
        // TODO: map to model and return domain object instead of the entity
        return eventMessageReceiverService.getAllMessages();
    }
}

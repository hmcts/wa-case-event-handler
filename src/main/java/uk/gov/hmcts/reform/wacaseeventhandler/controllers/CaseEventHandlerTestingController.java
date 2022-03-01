package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.EventMessageQueryResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.CaseEventMessageNotAllowedRequestException;
import uk.gov.hmcts.reform.wacaseeventhandler.services.EventMessageQueryService;
import uk.gov.hmcts.reform.wacaseeventhandler.services.EventMessageReceiverService;

import javax.validation.Valid;

@RestController
@Slf4j
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class CaseEventHandlerTestingController {
    private final EventMessageReceiverService eventMessageReceiverService;
    private final EventMessageQueryService eventMessageQueryService;

    @Value("${environment}")
    private String environment;

    public CaseEventHandlerTestingController(EventMessageReceiverService eventMessageReceiverService,
                                             EventMessageQueryService eventMessageQueryService) {
        this.eventMessageReceiverService = eventMessageReceiverService;
        this.eventMessageQueryService = eventMessageQueryService;
    }

    @Operation(summary = "Handles the CCD case event message")
    @ApiResponse(
        responseCode = "201",
        description = "Message processed successfully",
        content = @Content(schema = @Schema(implementation = Object.class))
    )
    @PostMapping(path = "/messages/{message_id}", consumes = {MediaType.APPLICATION_JSON_VALUE})
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    @ResponseStatus(HttpStatus.CREATED)
    public CaseEventMessage postCaseEventHandlerMessage(@Valid @RequestBody String message,
                                                        @PathVariable("message_id") final String messageId,
                                                        @RequestParam(value = "from_dlq",
                                                            required = false) final Boolean fromDlq) {
        if (isNonProdEnvironment()) {
            if (fromDlq != null && fromDlq) {
                return eventMessageReceiverService.handleDlqMessage(messageId, message);
            } else {
                return eventMessageReceiverService.handleCcdCaseEventAsbMessage(messageId, message);
            }
        } else {
            throw new CaseEventMessageNotAllowedRequestException();
        }
    }


    @Operation(summary = "Handles the CCD case event message")
    @ApiResponse(
        responseCode = "201",
        description = "Message processed successfully",
        content = @Content(schema = @Schema(implementation = Object.class))
    )
    @PutMapping(path = "/messages/{message_id}", consumes = {MediaType.APPLICATION_JSON_VALUE})
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    @ResponseStatus(HttpStatus.CREATED)
    public CaseEventMessage putCaseEventHandlerMessage(@Valid @RequestBody String message,
                                                       @PathVariable("message_id") final String messageId,
                                                       @RequestParam("from_dlq") final Boolean fromDlq) {
        if (isNonProdEnvironment()) {
            log.info("Processing '{}' in '{}' environment ", messageId, environment);
            return eventMessageReceiverService.upsertMessage(messageId, message, fromDlq);
        } else {
            throw new CaseEventMessageNotAllowedRequestException();
        }
    }

    @Operation(summary = "Gets the case event message by messageId")
    @ApiResponse(
        responseCode = "200",
        description = "Messages returned successfully",
        content = @Content(schema = @Schema(implementation = CaseEventMessageEntity.class))
    )
    @GetMapping("/messages/{message_id}")
    public CaseEventMessage getMessagesByMessageId(@PathVariable("message_id") final String messageId) {
        if (isNonProdEnvironment()) {
            return eventMessageReceiverService.getMessage(messageId);
        } else {
            throw new CaseEventMessageNotAllowedRequestException();
        }
    }

    @Operation(summary = "Gets the case event message by messageId")
    @ApiResponse(
        responseCode = "200",
        description = "Messages returned successfully",
        content = @Content(schema = @Schema(implementation = CaseEventMessageEntity.class)))
    @GetMapping("/messages/query")
    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public EventMessageQueryResponse getMessagesByQueryParameters(
        @RequestParam(value = "states", required = false) final String states,
        @RequestParam(value = "case_id", required = false) final String caseId,
        @RequestParam(value = "event_timestamp", required = false) final String eventTimestamp,
        @RequestParam(value = "from_dlq", required = false) final Boolean fromDlq) {

        if (isNonProdEnvironment()) {
            return eventMessageQueryService.getMessages(states, caseId, eventTimestamp, fromDlq);
        } else {
            throw new CaseEventMessageNotAllowedRequestException();
        }
    }

    @Operation(summary = "Deletes the case event message by messageId")
    @ApiResponse(
        responseCode = "200",
        description = "Messages deleted successfully"
    )
    @DeleteMapping("/messages/{message_id}")
    public void deleteMessageByMessageId(@PathVariable("message_id") final String messageId) {
        if (isNonProdEnvironment()) {
            eventMessageReceiverService.deleteMessage(messageId);
        } else {
            throw new CaseEventMessageNotAllowedRequestException();
        }
    }

    private boolean isNonProdEnvironment() {
        log.info("Processing message in '{}' environment ", environment);
        return !"prod".equalsIgnoreCase(environment);
    }
}

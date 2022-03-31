package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.services.ProblemMessageService;

import java.util.List;

@RestController
@Slf4j
public class ProblemMessageController {

    private final ProblemMessageService problemMessageService;

    public ProblemMessageController(ProblemMessageService problemMessageService) {
        this.problemMessageService = problemMessageService;
    }

    @Operation(description = "Query case event message db to find problematic messages by job name")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = List.class))})
    })
    @PostMapping(path = "/messages/jobs/{jobName}")
    public List<CaseEventMessage> findProblemMessages(@PathVariable String jobName) {
        log.info("Received request to find problem messages of type '{}'", jobName);
        List<CaseEventMessage> problemMessages = problemMessageService
            .findProblemMessages(JobName.valueOf(jobName));
        log.info("Retrieved messages: {}", problemMessages);
        return problemMessages;
    }
}

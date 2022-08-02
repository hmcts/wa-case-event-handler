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
import uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.services.jobservices.ProblemMessageService;

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
            @Content(mediaType = "application/json", schema = @Schema(implementation = JobResponse.class))})
    })
    @PostMapping(path = "/messages/jobs/{jobName}")
    public JobResponse problemMessagesJob(@PathVariable String jobName) {
        log.info("Received request to problem messages job of type '{}'", jobName);
        List<String> problemMessages = problemMessageService.process(JobName.valueOf(jobName));
        return new JobResponse(jobName, problemMessages == null ? 0 : problemMessages.size(), problemMessages);
    }
}

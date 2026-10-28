package uk.gov.hmcts.reform.wacaseeventhandler.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.ProblemMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;

@Slf4j
@Component
public class CaseEventMessageMapper {

    private final ObjectMapper objectMapper;

    public CaseEventMessageMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CaseEventMessage mapToCaseEventMessage(CaseEventMessageEntity entity) {
        if (entity == null) {
            return null;
        }

        return new CaseEventMessage(
            entity.getMessageId(),
            entity.getSequence(),
            entity.getCaseId(),
            entity.getEventTimestamp(),
            entity.getFromDlq(),
            entity.getState(),
            entity.getMessageProperties(),
            entity.getMessageContent(),
            entity.getReceived(),
            entity.getDeliveryCount(),
            entity.getHoldUntil(),
            entity.getRetryCount());
    }

    @SuppressWarnings("PMD.ConfusingTernary")
    private String getCaseTypeId(CaseEventMessageEntity entity) {
        String caseTypeId = null;
        if (entity.getMessageContent() != null && !entity.getMessageContent().isBlank()) {
            try {
                JsonNode jsonNodeMessageContent = objectMapper.readTree(entity.getMessageContent());
                JsonNode jsonNodeCaseTypeId = jsonNodeMessageContent.get("CaseTypeId");
                caseTypeId = jsonNodeCaseTypeId.asText();
            } catch (JacksonException jsonProcessingException) {
                log.info("Error extracting CaseTypeId from message", jsonProcessingException);
            }
        } else {
            log.warn("messageContent is null or empty for messageId: {}", entity.getMessageId());
        }
        return caseTypeId;
    }

    public ProblemMessage mapToProblemMessage(CaseEventMessageEntity entity) {
        if (entity == null) {
            return null;
        }

        String caseTypeId = getCaseTypeId(entity);

        return new ProblemMessage(
            entity.getMessageId(),
            entity.getCaseId(),
            caseTypeId,
            entity.getEventTimestamp(),
            entity.getFromDlq(),
            entity.getState());
    }
}

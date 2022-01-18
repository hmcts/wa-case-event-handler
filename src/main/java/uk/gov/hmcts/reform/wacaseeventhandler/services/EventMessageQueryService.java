package uk.gov.hmcts.reform.wacaseeventhandler.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.EventMessageQueryResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.InvalidRequestParametersException;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageCustomCriteriaRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static net.logstash.logback.encoder.org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;


@Slf4j
@Service
@Transactional
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public class EventMessageQueryService {

    protected static final String NO_RECORDS_IN_THE_DATABASE = "There are no records in the database";
    protected static final String NO_QUERY_PARAMETERS_SPECIFIED = "No query parameters specified";
    protected static final String FOUND_MESSAGES = "Found %s messages";
    protected static final String NO_MATCHING_RECORDS_FOR_THE_QUERY = "No records matching the query";

    private final CaseEventMessageCustomCriteriaRepository customCriteriaRepository;
    private final CaseEventMessageMapper mapper;

    public EventMessageQueryService(CaseEventMessageCustomCriteriaRepository customCriteriaRepository,
                                    CaseEventMessageMapper caseEventMessageMapper) {
        this.customCriteriaRepository = customCriteriaRepository;
        this.mapper = caseEventMessageMapper;
    }

    public EventMessageQueryResponse getMessages(String states, String caseId, String eventTimestamp, String fromDlq) {

        long numberOfAllMessages = customCriteriaRepository.countAll();
        if (numberOfAllMessages == 0) {
            return new EventMessageQueryResponse(NO_RECORDS_IN_THE_DATABASE, 0, 0, emptyList());
        }
        if (isBlank(states) && isBlank(caseId) && isBlank(eventTimestamp) && isBlank(fromDlq)) {
            return new EventMessageQueryResponse(NO_QUERY_PARAMETERS_SPECIFIED, numberOfAllMessages, 0, emptyList());
        }

        String decodedStates = decode(states);
        validateGetMessagesParameters(decodedStates, caseId, eventTimestamp, fromDlq);

        List<CaseEventMessageEntity> messageEntities = customCriteriaRepository.getMessages(
            messageStates(splitStates(decodedStates)),
            caseId,
            isBlank(eventTimestamp) ? null : LocalDateTime.parse(eventTimestamp),
            isBlank(fromDlq) ? null : Boolean.valueOf(fromDlq)
        );
        List<CaseEventMessage> messages = messageEntities.stream()
            .map(mapper::mapToCaseEventMessage).collect(Collectors.toList());

        String message = messages.isEmpty() ? NO_MATCHING_RECORDS_FOR_THE_QUERY
            : format(FOUND_MESSAGES, messages.size());
        return new EventMessageQueryResponse(message, numberOfAllMessages, messages.size(), messages);
    }

    private String decode(String states) {
        if (isBlank(states)) {
            return states;
        } else {
            return states.replace("%2C", ",");
        }
    }

    private void validateGetMessagesParameters(String states, String caseId, String eventTimestamp, String fromDlq) {
        validateStates(states);
        validateCaseId(caseId);
        validateEventTimestamp(eventTimestamp);
        validateFromDlq(fromDlq);
    }

    private void validateFromDlq(String fromDlq) {
        if (isNotBlank(fromDlq) && !"true".equalsIgnoreCase(fromDlq) && !"false".equalsIgnoreCase(fromDlq)) {
            throw new InvalidRequestParametersException(format("Invalid from_dlq format: '%s'", fromDlq));
        }
    }

    private void validateEventTimestamp(String eventTimestamp) {
        if (isNotBlank(eventTimestamp)) {
            try {
                LocalDateTime.parse(eventTimestamp);
            } catch (DateTimeParseException e) {
                throw new InvalidRequestParametersException(
                    format("Invalid event_timestamp format: '%s'", eventTimestamp), e);
            }
        }
    }

    private void validateCaseId(String caseId) {
        if (caseId != null && caseId.replaceAll("[^\\d]", "").length() != 16) {
            throw new InvalidRequestParametersException(format("Invalid case_id format: '%s'", caseId));
        }
    }

    private void validateStates(String states) {
        if (isNotBlank(states)) {
            List<String> statesList = splitStates(states);
            statesList.forEach(state -> {
                try {
                    MessageState.valueOf(state);
                } catch (IllegalArgumentException e) {
                    throw new InvalidRequestParametersException(format("Invalid states format: '%s'", states), e);
                }
            });
        }
    }

    private List<String> splitStates(String states) {
        return isBlank(states) ? emptyList() : asList(states.split(",", 0));
    }

    private List<MessageState> messageStates(List<String> statesList) {
        return statesList.stream()
            .filter(Objects::nonNull)
            .map(String::toUpperCase)
            .map(MessageState::valueOf)
            .collect(Collectors.toList());
    }

}

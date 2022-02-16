package uk.gov.hmcts.reform.wacaseeventhandler.services;

import com.microsoft.applicationinsights.core.dependencies.apachecommons.codec.digest.DigestUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;

@Slf4j
@Service
public class IdempotencyKeyGenerator {

    public String generateIdempotencyKey(String messageId, String eventId) {
        log.debug("Generating idempotency key for message: {} and event: {}", messageId, eventId);
        Objects.requireNonNull(messageId, "messageId cannot be null");
        Objects.requireNonNull(eventId, "eventId cannot be null");
        String concatenatedString = messageId + eventId;
        String idempotencyKey = DigestUtils.md5Hex(concatenatedString).toUpperCase(Locale.ENGLISH);
        log.debug("Idempotency key generated: {}", idempotencyKey);
        return idempotencyKey;
    }

}

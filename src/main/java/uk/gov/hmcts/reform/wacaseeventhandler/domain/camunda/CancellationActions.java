package uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda;

import static java.util.Arrays.stream;

public enum CancellationActions {

    CANCEL,
    WARN,
    RECONFIGURE;

    public static CancellationActions from(String value) {
        return stream(values())
            .filter(e -> e.name().equalsIgnoreCase(value)).findAny().orElse(null);
    }
}

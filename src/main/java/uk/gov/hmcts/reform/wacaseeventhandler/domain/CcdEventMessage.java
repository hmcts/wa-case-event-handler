package uk.gov.hmcts.reform.wacaseeventhandler.domain;

import lombok.Builder;

import javax.validation.constraints.NotBlank;

@Builder
public class CcdEventMessage {

    @NotBlank
    private final String id;
    private final String name;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

}

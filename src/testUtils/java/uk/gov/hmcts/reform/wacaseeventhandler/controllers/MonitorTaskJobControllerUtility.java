package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import java.util.function.Function;

public final class MonitorTaskJobControllerUtility {

    public static Function<String, String> expectedResponse = (name) -> "{\"job_details\":{\"name\":\"" + name + "\"}}";

    private MonitorTaskJobControllerUtility() {
        // utility class should not have a public or default constructor
    }

}
